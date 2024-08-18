package com.atchen.AISearch.controller;

import com.atchen.AISearch.entity.Comment;
import com.atchen.AISearch.service.ICommentService;
import com.atchen.AISearch.utils.ResultEntity;
import com.atchen.AISearch.utils.SecurityUtil;
import com.atchen.AISearch.utils.idempotent.Idempotent;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-14
 * @Description: 评论控制器
 * @Version: 1.0
 */

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Resource
    private ICommentService commentService;
    /*
        * @description 添加评论
        * @author atchen
        * @date 2024/8/14
    */
    @PostMapping("/add")
    @Idempotent
    public ResultEntity add(@Validated Comment comment) {

        comment.setUid(SecurityUtil.getUserDetails().getUid());
        boolean save = commentService.save(comment);
        if (save){
            return ResultEntity.success(save);
        }
        return ResultEntity.fail("添加评论失败");
    }
}
    