package com.atchen.AISearch.service.impl;

import com.atchen.AISearch.entity.Comment;
import com.atchen.AISearch.entity.vo.CommentVO;
import com.atchen.AISearch.mapper.CommentMapper;
import com.atchen.AISearch.service.ICommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author atchen
 * @since 2024-08-14
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {
    @Resource
    private CommentMapper commentMapper;
    @Override
    public List<CommentVO> getCommentList(Long did) {
       return commentMapper.getCommentList(did);
    }
}
