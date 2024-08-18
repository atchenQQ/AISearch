package com.atchen.AISearch.service.impl;

import com.atchen.AISearch.entity.User;
import com.atchen.AISearch.mapper.UserMapper;
import com.atchen.AISearch.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author atchen
 * @since 2024-08-10
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private UserMapper userMapper;

    @Override
    public int updateUsecount(Long uid) {
        return userMapper.updateUsecount(uid);
    }
}
