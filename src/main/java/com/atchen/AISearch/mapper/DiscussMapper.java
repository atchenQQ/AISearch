package com.atchen.AISearch.mapper;

import com.atchen.AISearch.entity.Discuss;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author atchen
 * @since 2024-08-14
 */
public interface DiscussMapper extends BaseMapper<Discuss> {
    @Update("update discuss set readcount = readcount + 1 where did = #{did}")
    int updateReadcount(@RequestParam("did") Long id);

    @Update("update discuss set supportcount = supportcount + 1 where did = #{did}")
    int updateSupportcount(@RequestParam("did")Long did);
}
