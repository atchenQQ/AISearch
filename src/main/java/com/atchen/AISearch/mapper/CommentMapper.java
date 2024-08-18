package com.atchen.AISearch.mapper;

import com.atchen.AISearch.entity.Comment;
import com.atchen.AISearch.entity.vo.CommentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author atchen
 * @since 2024-08-14
 */
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("select c.*,u.username from `comment` c  left join `user` u on c.uid=u.uid\n" +
            "where c.did = #{did} ORDER BY c.cid DESC")
    List<CommentVO> getCommentList(@RequestParam("did") Long did);

}
