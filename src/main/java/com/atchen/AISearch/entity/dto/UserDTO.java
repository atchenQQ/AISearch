package com.atchen.AISearch.entity.dto;

import com.atchen.AISearch.entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-10
 * @Description: user传输对象
 * @Version: 1.0
 */

@Data
public class UserDTO extends User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1222222222221L;
    @NotBlank(message = "验证码不能为空")
    private String captcha; // 验证码
}
    