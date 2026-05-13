package com.tihu.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tihu.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}