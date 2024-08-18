package com.atchen.AISearch.service.impl;

import com.atchen.AISearch.entity.Discuss;
import com.atchen.AISearch.mapper.DiscussMapper;
import com.atchen.AISearch.service.IDiscussService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author atchen
 * @since 2024-08-14
 */
@Service
public class DiscussServiceImpl extends ServiceImpl<DiscussMapper, Discuss> implements IDiscussService {

    @Resource
    private DiscussMapper discussMapper;
    @Override
    public int updateReadcount(Long id) {
        return discussMapper.updateReadcount(id);
    }

    @Override
    public int updateSupportcount(Long did) {
      return   discussMapper.updateSupportcount(did);
    }
}
