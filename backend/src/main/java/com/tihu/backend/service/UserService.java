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
     * 修改当前用户资料
     */
    User updateProfile(Long userId, String currentUsername, String newUsername, String newPassword, String avatar) throws Exception;

    /**
     * 封禁用户
     */
    void banUser(Long userId, Long banDurationSeconds);

    /**
     * 解封用户
     */
    void unbanUser(Long userId);

    /**
     * 按用户名封禁用户
     */
    void banUserByUsername(String username, String until);

    /**
     * 按用户名解封用户
     */
    void unbanUserByUsername(String username);

    /**
     * 获取用户主页信息（包含评论、书单、关注等）
     */
    Object getUserProfile(String username);

    /**
     * 获取管理员封禁列表
     */
    Object getBanList();
}
