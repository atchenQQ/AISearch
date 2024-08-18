package com.atchen.AISearch.controller;

import cn.hutool.core.bean.BeanUtil;
import com.atchen.AISearch.entity.Discuss;
import com.atchen.AISearch.entity.DiscussSupport;
import com.atchen.AISearch.entity.User;
import com.atchen.AISearch.entity.vo.CommentVO;
import com.atchen.AISearch.entity.vo.DiscussVO;
import com.atchen.AISearch.service.ICommentService;
import com.atchen.AISearch.service.IDiscussService;
import com.atchen.AISearch.service.IDiscussSupportService;
import com.atchen.AISearch.service.IUserService;
import com.atchen.AISearch.utils.AppVariable;
import com.atchen.AISearch.utils.ResultEntity;
import com.atchen.AISearch.utils.SecurityUtil;
import com.atchen.AISearch.utils.idempotent.Idempotent;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-14
 * @Description: 讨论表控制器
 * @Version: 1.0
 */

@RestController
@RequestMapping("/discuss")
public class DiscussController {
    @Resource
    private IDiscussService discussService;
    @Resource
    private ThreadPoolTaskExecutor threadPool;
    @Resource
    private IUserService  userService;
    @Resource
    private ICommentService commentService;
    @Resource
    private KafkaTemplate kafkaTemplate;
    @Resource
    private IDiscussSupportService discussSupportService;


    /*
        * @description  新增讨论
        * @author atchen
        * @date 2024/8/14
    */
    @PostMapping("/add")
    public ResultEntity add(@Validated Discuss discuss) {
        discuss.setUid(SecurityUtil.getUserDetails().getUid());
        boolean saveResult = discussService.save(discuss);
        if (saveResult) {
            return ResultEntity.success(saveResult);
        } else {
            return ResultEntity.fail("添加失败,请稍后再试");
        }
    }

    /*
        * @description  查询我的所有讨论
        * @author atchen
        * @date 2024/8/14
    */
    @PostMapping("/mylist")
    public ResultEntity mylist() {
        return ResultEntity.success(discussService.list(
                Wrappers.lambdaQuery(Discuss.class)
                       .eq(Discuss::getUid, SecurityUtil.getUserDetails().getUid())
                       .orderByDesc(Discuss::getDid)
        ));
    }
    /*
        * @description 删除个人话题
        * @author atchen
        * @date 2024/8/14
    */
    @PostMapping("/delete")
    public ResultEntity delete(Long did) {
        if (did == null || did <= 0) {
            return ResultEntity.fail("参数错误");
        }
        boolean removeResult = discussService.remove(Wrappers.lambdaQuery(Discuss.class)
               .eq(Discuss::getDid, did)
               .eq(Discuss::getUid, SecurityUtil.getUserDetails().getUid())
        );

        if (removeResult) {
            return ResultEntity.success(removeResult);
        }
        return ResultEntity.fail("删除失败,未知错误！");
     }

     /*
         * @description 查询讨论详情
         * @author atchen
         * @date 2024/8/14
     */
     @PostMapping("/detail")
     public ResultEntity detail(Long did) throws ExecutionException, InterruptedException {
         if (did == null || did <= 0) {
             return ResultEntity.fail("参数错误");
         }
         Discuss discuss = discussService.getById(did);
         if (discuss != null && discuss.getDid()>0) {
         // 添加查询量

             threadPool.submit(() -> {
                //更新数据库
                 discussService.updateReadcount(did);
                 discuss.setReadcount(discuss.getReadcount()+1);
             });

         // 任务1 查询discuss中的 username
         CompletableFuture<DiscussVO> task1 = CompletableFuture.supplyAsync(() -> {
             DiscussVO discussVO = BeanUtil.toBean(discuss, DiscussVO.class);
             User user = userService.getById(discuss.getUid());
             if (user != null && user.getUid() > 0){
                 discussVO.setUsername(user.getUsername());
             }
             return discussVO;
         }, threadPool);

         // 任务2 查询discuss对应的Comment表
         CompletableFuture<List<CommentVO>> task2 = CompletableFuture.supplyAsync(() -> {
             return commentService.getCommentList(did);
         }, threadPool);
         // 等待所有任务完成
         CompletableFuture allTask = CompletableFuture.allOf(task1, task2);
         HashMap<String, Object> resultMap = new HashMap<>();
         resultMap.put("discuss", task1.get());
         resultMap.put("commentlist", task2.get());
         return ResultEntity.success(resultMap);
         }
         return ResultEntity.fail("讨论不存在");
     }

     /*
         * @description 讨论表点赞事件
         * @author atchen
         * @date 2024/8/14
     */

     @PostMapping("/support")
     @Idempotent
     public ResultEntity support(Long did) {
         if (did == null || did <= 0) {
             return ResultEntity.fail("参数错误");
         }
         // kafka 异步处理
         kafkaTemplate.send(AppVariable.DISCUSS_SUPPORT_TOPIC, did+"_"+SecurityUtil.getUserDetails().getUid());
         return ResultEntity.success(true);
     }

     /*
         * @description  监听kafka中的点赞事件
         * @author atchen
         * @date 2024/8/14
     */
     @KafkaListener(topics = {AppVariable.DISCUSS_SUPPORT_TOPIC})
     public void supportEvent(String data, Acknowledgment acknowledgment) {
         // 0 判断用户未给该讨论表点赞过
         String[] split = data.split("_");
         Long did = Long.parseLong(split[0]);
         Long uid = Long.parseLong(split[1]);
         List<DiscussSupport> supportList = discussSupportService.list(Wrappers.lambdaQuery(DiscussSupport.class)
                .eq(DiscussSupport::getDid, did)
                .eq(DiscussSupport::getUid, uid)
         );
         if (supportList==null || supportList.size()==0){
             // 1 修改讨论表中的总点赞数
             int result = discussService.updateSupportcount(did);  // result 为影响的行数
             if (result > 0){       
                 // 2 在点赞表中添加点赞记录
                 DiscussSupport discussSupport = new DiscussSupport();
                 discussSupport.setDid(did);
                 discussSupport.setUid(uid);
                 discussSupportService.save(discussSupport);
             }
         }
         // 手动确认消息已被消费，可以安全的丢弃
         acknowledgment.acknowledge();
     }

     /*
         * @description 获取讨论列表，分页
         * @author atchen  1 = 推荐(根据点赞数排序) ;2 = 最新(根据时间排序)
         * @date 2024/8/15
     */
    @PostMapping("/list")
    public ResultEntity list(Integer page, Integer type) {
        // 参数预处理
        if (page == null || page <= 0) page = 1;
        if (type == null || type <= 0) type = 1;
        QueryWrapper<Discuss> queryWrapper = new QueryWrapper<>();
        if (type == 1){
            // 推荐排序
            queryWrapper.orderByDesc("supportcount");
        }
        else {
            // 最新排序
            queryWrapper.orderByDesc("did");
        }
        Page<Discuss> result = discussService.page(new Page<>(page, AppVariable.PAGE_SIZE), queryWrapper);
        return ResultEntity.success(result);
    }





}
    