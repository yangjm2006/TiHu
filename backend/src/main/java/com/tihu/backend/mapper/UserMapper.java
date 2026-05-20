package com.tihu.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tihu.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 按用户名查询用户
     */
    @Select("SELECT * FROM `user` WHERE username = #{username} AND is_deleted = 0")
    User selectByUsername(String username);
}