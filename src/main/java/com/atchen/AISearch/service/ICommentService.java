package com.atchen.AISearch.service;

import com.atchen.AISearch.entity.Comment;
import com.atchen.AISearch.entity.vo.CommentVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author atchen
 * @since 2024-08-14
 */
public interface ICommentService extends IService<Comment> {
    List<CommentVO> getCommentList(Long did);
}
