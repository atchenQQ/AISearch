package com.atchen.AISearch.entity.vo;

import com.atchen.AISearch.entity.Discuss;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-14
 * @Description: discuss传输对象
 * @Version: 1.0
 */

@Data
public class DiscussVO extends Discuss implements Serializable {
      @Serial
      private static final long serialVersionUID = 2222222222131L;
      private String username;
}
    