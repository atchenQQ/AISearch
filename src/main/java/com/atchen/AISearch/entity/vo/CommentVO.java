package com.atchen.AISearch.entity.vo;

import com.atchen.AISearch.entity.Comment;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-14
 * @Description: 评论表VO对象
 * @Version: 1.0
 */

@Data
public class CommentVO extends Comment implements Serializable {
    @Serial
    private static final long serialVersionUID = 1444449412412L;
    private String username;
}
    