package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.User;
import com.tihu.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关接口
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     * POST /api/users/register
     */
    @PostMapping("/register")
    public Result register(
            @RequestBody(required = false) User user,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false, defaultValue = "") String inviteCode) throws Exception {
        String finalUsername = resolveField(user != null ? user.getUsername() : null, username);
        String finalPassword = resolveField(user != null ? user.getPassword() : null, password);
        User registered = userService.register(finalUsername, finalPassword, inviteCode);
        return Result.success(registered);
    }

    /**
     * 用户登录
     * POST /api/users/login
     */
    @PostMapping("/login")
    public Result login(@RequestBody(required = false) User user,
                        @RequestParam(required = false) String username,
                        @RequestParam(required = false) String password) throws Exception {
        String finalUsername = resolveField(user != null ? user.getUsername() : null, username);
        String finalPassword = resolveField(user != null ? user.getPassword() : null, password);
        User loggedIn = userService.login(finalUsername, finalPassword);
        String token = StpUtil.getTokenValue();

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("userInfo", loggedIn);
        data.put("token", token);
        return Result.success(data);
    }

    /**
     * 用户登出
     * POST /api/users/logout
     */
    @PostMapping("/logout")
    public Result logout() {
        StpUtil.logout();
        return Result.success();
    }

    /**
     * 获取当前用户信息
     * GET /api/users/me
     */
    @GetMapping("/me")
    public Result getCurrentUser() {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        User user = userService.getById(userId);
        return Result.success(user);
    }

    /**
     * 修改密码
     * PUT /api/users/{id}/password
     */
    @PutMapping("/{id}/password")
    public Result updatePassword(@PathVariable Long id, @RequestParam String oldPassword, @RequestParam String newPassword) throws Exception {
        userService.updatePassword(id, oldPassword, newPassword);
        return Result.success();
    }

    /**
     * 修改用户名
     * PUT /api/users/{id}/username
     */
    @PutMapping("/{id}/username")
    public Result updateUsername(@PathVariable Long id, @RequestParam String newUsername) throws Exception {
        userService.updateUsername(id, newUsername);
        return Result.success();
    }

    /**
     * 按用户名获取用户信息
     * GET /api/users/by-username/{username}
     */
    @GetMapping("/by-username/{username}")
    public Result getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.error(404, "用户不存在", null);
        }
        return Result.success(user);
    }

    /**
     * 按用户ID或用户名获取用户信息
     * GET /api/users/{id}
     * GET /api/users/{username}
     */
    @GetMapping("/{identifier}")
    public Result getUserByIdentifier(@PathVariable String identifier) {
        User user;
        if (identifier.matches("\\d+")) {
            user = userService.getById(Long.parseLong(identifier));
        } else {
            user = userService.getUserByUsername(identifier);
        }
        if (user == null) {
            return Result.error(404, "用户不存在", null);
        }
        return Result.success(user);
    }

    /**
     * 获取用户主页信息（包含评论、书单、关注等）
     * GET /api/users/profile/{username}
     */
    @GetMapping("/profile/{username}")
    public Result getUserProfile(@PathVariable String username) {
        Object profile = userService.getUserProfile(username);
        if (profile == null) {
            return Result.error(404, "用户不存在", null);
        }
        return Result.success(profile);
    }

    /**
     * 管理员接口：获取封禁列表
     * GET /api/users/admin/bans
     */
    @GetMapping("/admin/bans")
    public Result getBanList() {
        Object bans = userService.getBanList();
        return Result.success(bans);
    }

    /**
     * 管理员接口：封禁用户
     * POST /api/users/admin/{id}/ban
     */
    @PostMapping("/admin/{id}/ban")
    public Result banUser(@PathVariable Long id, @RequestParam Long durationSeconds) {
        userService.banUser(id, durationSeconds);
        return Result.success();
    }

    /**
     * 管理员接口：解封用户
     * DELETE /api/users/admin/{id}/ban
     */
    @DeleteMapping("/admin/{id}/ban")
    public Result unbanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return Result.success();
    }

    private String resolveField(String bodyValue, String requestParamValue) {
        return bodyValue != null && !bodyValue.isBlank() ? bodyValue : requestParamValue;
    }
}
