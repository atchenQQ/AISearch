package com.atchen.AISearch.mapper;

import com.atchen.AISearch.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author atchen
 * @since 2024-08-10
 */
public interface UserMapper extends BaseMapper<User> {
    @Update("UPDATE `user` SET usecount = usecount -1 WHERE uid  = #{uid}")
    int updateUsecount(@RequestParam("uid") Long uid);
}
