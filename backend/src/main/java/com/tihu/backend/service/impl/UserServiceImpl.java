package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.UserMapper;
import com.tihu.backend.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
