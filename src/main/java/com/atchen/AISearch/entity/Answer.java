package com.atchen.AISearch.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 * 大模型查询结果
 * @author atchen
 * @since 2024-08-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("answer")

public class Answer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "aid", type = IdType.AUTO)
    private Long aid;

    /**
     * 问题
     */
    private String title;

    public Answer() {
    }

    /**
     * 答案
     */
    private String content;

    /**
     * 大模型类型，1=openai，2 = tongyi，3=讯飞，4=文心，5=豆包，6=其他
     */
    private Integer model;

    private String createtime;

    private String updatetime;

    private Long uid;

    /**
     * 类型 1= 对话 2 = 绘图
     */
    private Integer type;


}
