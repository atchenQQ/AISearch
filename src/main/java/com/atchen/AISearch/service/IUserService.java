package com.atchen.AISearch.service;

import com.atchen.AISearch.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author atchen
 * @since 2024-08-10
 */
public interface IUserService extends IService<User> {
    int updateUsecount( Long uid);
}
