package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.entity.User;

/**
 * 用户Service
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     */
    User register(String username, String password, String inviteCode) throws Exception;

    /**
     * 用户登录
     */
    User login(String username, String password) throws Exception;

    /**
     * 按用户名查询用户
     */
    User getUserByUsername(String username);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword) throws Exception;

    /**
     * 修改用户名
     */
    void updateUsername(Long userId, String newUsername) throws Exception;

    /**
     * 封禁用户
     */
    void banUser(Long userId, Long banDurationSeconds);

    /**
     * 解封用户
     */
    void unbanUser(Long userId);
}
