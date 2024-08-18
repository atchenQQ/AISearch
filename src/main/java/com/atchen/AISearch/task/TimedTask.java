package com.atchen.AISearch.task;

import com.atchen.AISearch.entity.User;
import com.atchen.AISearch.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-15
 * @Description: 用户使用次数定时任务
 * @Version: 1.0
 */

/*
    * @description 存放定时任务
    * @author atchen
    * @date 2024/8/15
*/
@Component
public class TimedTask {
    @Resource
    private IUserService userService;
    @Value("${system.user.use-count}")
    private Integer useCount;
    /*
        * @description  重置用户使用次数定时任务
        * @author atchen
        * @date 2024/8/15
    */
    @XxlJob("resetUserUseCount")
    public void resetUserUseCount() {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("usecount",useCount);

        if (!userService.update(updateWrapper)) {
            // todo 调用通知中心进行排查
        }
    }





}
    