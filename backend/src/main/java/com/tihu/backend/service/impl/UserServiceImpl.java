package com.tihu.backend.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.Constants;
import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.UserMapper;
import com.tihu.backend.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户Service实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z])[a-zA-Z0-9]{6,12}$");

    /**
     * 用户注册
     */
    @Override
    public User register(String username, String password, String inviteCode) throws Exception {
        // 验证用户名
        if (username == null || username.length() < Constants.USERNAME_MIN_LENGTH || username.length() > Constants.USERNAME_MAX_LENGTH) {
            throw new ApiException(400, "用户名长度必须在2-10之间");
        }
        
        // 检查用户名唯一性
        User existing = this.getUserByUsername(username);
        if (existing != null) {
            throw new ApiException(409, "用户名已存在");
        }
        
        // 验证密码
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ApiException(400, "密码必须为6-12位，包含数字和英文字符");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        
        // 判断是否注册为管理员
        if (Constants.ADMIN_INVITE_CODE.equals(inviteCode)) {
            user.setRole(Constants.ROLE_ADMIN);
        } else {
            user.setRole(Constants.ROLE_USER);
        }
        
        user.setStatus(0); // 正常状态
        user.setIsDeleted(0);
        
        this.save(user);
        return user;
    }

    /**
     * 用户登录
     */
    @Override
    public User login(String username, String password) throws Exception {
        User user = this.getUserByUsername(username);
        if (user == null) {
            throw new ApiException(401, "用户名或密码错误");
        }
        
        // 检查用户是否被封禁
        if (user.getStatus() == 1) {
            if (user.getBanExpireTime() != null && LocalDateTime.now().isBefore(user.getBanExpireTime())) {
                throw new ApiException(403, "该用户已被封禁");
            } else if (user.getBanExpireTime() != null) {
                // 解封
                user.setStatus(0);
                user.setBanExpireTime(null);
                this.updateById(user);
            }
        }
        
        // 验证密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        
        // 使用Sa-Token登录
        StpUtil.login(user.getId());

        return user;
    }

    /**
     * 按用户名查询用户
     */
    @Override
    public User getUserByUsername(String username) {
        return this.baseMapper.selectByUsername(username);
    }

    /**
     * 修改密码
     */
    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        
        // 验证旧密码
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new ApiException(401, "旧密码错误");
        }
        
        // 验证新密码格式
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new ApiException(400, "新密码必须为6-12位，包含数字和英文字符");
        }
        
        user.setPassword(BCrypt.hashpw(newPassword));
        this.updateById(user);
    }

    /**
     * 修改用户名
     */
    @Override
    public void updateUsername(Long userId, String newUsername) throws Exception {
        if (newUsername == null || newUsername.length() < Constants.USERNAME_MIN_LENGTH || newUsername.length() > Constants.USERNAME_MAX_LENGTH) {
            throw new ApiException(400, "用户名长度必须在2-10之间");
        }
        
        User existing = this.getUserByUsername(newUsername);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new ApiException(409, "用户名已存在");
        }
        
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        
        user.setUsername(newUsername);
        this.updateById(user);
    }

    /**
     * 封禁用户
     */
    @Override
    public void banUser(Long userId, Long banDurationSeconds) {
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        
        user.setStatus(1);
        user.setBanExpireTime(LocalDateTime.now().plusSeconds(banDurationSeconds));
        this.updateById(user);
    }

    /**
     * 解封用户
     */
    @Override
    public void unbanUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        
        user.setStatus(0);
        user.setBanExpireTime(null);
        this.updateById(user);
    }
}


