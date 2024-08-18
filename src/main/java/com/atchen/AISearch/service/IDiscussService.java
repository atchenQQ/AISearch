package com.atchen.AISearch.service;

import com.atchen.AISearch.entity.Discuss;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author atchen
 * @since 2024-08-14
 */
public interface IDiscussService extends IService<Discuss> {
    int updateReadcount( Long id);

    int updateSupportcount(Long did);
}
