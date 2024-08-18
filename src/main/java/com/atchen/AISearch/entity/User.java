package com.atchen.AISearch.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author atchen
 * @since 2024-08-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "uid", type = IdType.AUTO)
    private Long uid;

    /**
     * 用户名
     */

    @NotBlank(message = "用户名不能为空")  // 验证用户名不能为空
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")  // 验证密码不能为空
    private String password;

    /**
     * 头像
     */
    private String photo;

    /**
     * 状态，预留字段
     */
    private Integer state;

    /**
     * 创建时间
     */
    private String createtime;

    /**
     * 修改时间
     */
    private String  updatetime;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 明天可以使用大模型的次数
     */
    private Integer usecount;


}
