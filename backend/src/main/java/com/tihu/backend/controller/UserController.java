package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.PageData;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.User;
import com.tihu.backend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                        @RequestParam(required = false) String password,
                        HttpServletResponse response) throws Exception {
        String finalUsername = resolveField(user != null ? user.getUsername() : null, username);
        String finalPassword = resolveField(user != null ? user.getPassword() : null, password);
        User loggedIn = userService.login(finalUsername, finalPassword);
        String token = StpUtil.getTokenValue();
        response.setHeader("Authorization", token);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("userInfo", loggedIn);
        data.put("token", token);
        data.put("role", loggedIn.getRole());
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
     * 修改当前用户资料
     * PUT /api/users/profile
     * PUT /api/users/me
     */
    @PutMapping({"/profile", "/me"})
    public Result updateProfile(@RequestBody(required = false) Map<String, Object> body) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        String currentUsername = textValue(body, "currentUsername", "username");
        String newUsername = textValue(body, "newUsername");
        String newPassword = textValue(body, "newPassword");
        String avatar = textValue(body, "avatarImage", "avatar", "avatarUrl", "profileImage", "profileImageUrl");
        User user = userService.updateProfile(userId, currentUsername, newUsername, newPassword, avatar);
        return Result.success(user);
    }

    /**
     * 修改密码
     * PUT /api/users/{id}/password
     */
    @PutMapping("/{id}/password")
    public Result updatePassword(@PathVariable Long id, @RequestParam String oldPassword, @RequestParam String newPassword) throws Exception {
        checkSelfOrAdmin(id);
        userService.updatePassword(id, oldPassword, newPassword);
        return Result.success();
    }

    /**
     * 修改用户名
     * PUT /api/users/{id}/username
     */
    @PutMapping("/{id}/username")
    public Result updateUsername(@PathVariable Long id, @RequestParam String newUsername) throws Exception {
        checkSelfOrAdmin(id);
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
        StpUtil.checkRole("ADMIN");
        return Result.success(toBanPage(userService.getBanList(), 1, 1000));
    }

    /**
     * 管理员接口：查询全部用户
     * GET /api/users/admin?page=1&size=1000&sort=created_at_desc
     */
    @GetMapping("/admin")
    public Result getAdminUsers(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "1000") int size,
                                @RequestParam(defaultValue = "created_at_desc") String sort) {
        StpUtil.checkRole("ADMIN");
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
            .eq(User::getIsDeleted, 0);
        if ("created_at_asc".equalsIgnoreCase(sort)) {
            wrapper.orderByAsc(User::getCreateTime).orderByAsc(User::getId);
        } else {
            wrapper.orderByDesc(User::getCreateTime).orderByDesc(User::getId);
        }
        Page<User> userPage = userService.page(new Page<>(Math.max(1, page), Math.max(1, size)), wrapper);
        return Result.success(toAdminUserPage(userPage));
    }

    /**
     * 管理员接口：获取封禁列表
     * GET /api/users/bans?page=1&size=1000
     */
    @GetMapping("/bans")
    public Result getBanList(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "1000") int size) {
        StpUtil.checkRole("ADMIN");
        return Result.success(toBanPage(userService.getBanList(), page, size));
    }

    /**
     * 管理员接口：封禁用户
     * POST /api/users/ban?username=alice&until=2026-06-01T12:00:00
     */
    @PostMapping("/ban")
    public Result banUserByUsername(@RequestParam String username,
                                    @RequestParam(required = false) String until) {
        StpUtil.checkRole("ADMIN");
        userService.banUserByUsername(username, until);
        return Result.success();
    }

    /**
     * 管理员接口：解封用户
     * POST /api/users/unban?username=alice
     */
    @PostMapping("/unban")
    public Result unbanUserByUsername(@RequestParam String username) {
        StpUtil.checkRole("ADMIN");
        userService.unbanUserByUsername(username);
        return Result.success();
    }

    /**
     * 管理员接口：赋予管理员权限
     * POST /api/users/grant-admin?username=alice
     */
    @PostMapping("/grant-admin")
    public Result grantAdmin(@RequestParam String username) {
        StpUtil.checkRole("ADMIN");
        userService.grantAdminByUsername(username);
        return Result.success();
    }

    /**
     * 管理员接口：封禁用户
     * POST /api/users/admin/{id}/ban
     */
    @PostMapping("/admin/{id}/ban")
    public Result banUser(@PathVariable Long id, @RequestParam Long durationSeconds) {
        StpUtil.checkRole("ADMIN");
        userService.banUser(id, durationSeconds);
        return Result.success();
    }

    /**
     * 管理员接口：解封用户
     * DELETE /api/users/admin/{id}/ban
     */
    @DeleteMapping("/admin/{id}/ban")
    public Result unbanUser(@PathVariable Long id) {
        StpUtil.checkRole("ADMIN");
        userService.unbanUser(id);
        return Result.success();
    }

    private String resolveField(String bodyValue, String requestParamValue) {
        return bodyValue != null && !bodyValue.isBlank() ? bodyValue : requestParamValue;
    }

    private void checkSelfOrAdmin(Long targetUserId) {
        Long currentUserId = Long.parseLong(StpUtil.getLoginId().toString());
        if (!currentUserId.equals(targetUserId)) {
            StpUtil.checkRole("ADMIN");
        }
    }

    private String textValue(Map<String, Object> body, String... names) {
        if (body == null) {
            return null;
        }
        for (String name : names) {
            Object value = body.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private PageData<Object> toBanPage(Object source, int page, int size) {
        if (!(source instanceof List<?> users)) {
            throw new ApiException(500, "封禁列表格式错误");
        }
        List<Object> records = users.stream().map(item -> {
            User user = (User) item;
            Map<String, Object> record = new HashMap<>();
            record.put("username", user.getUsername());
            record.put("user", user.getUsername());
            record.put("bannedUntil", user.getBanExpireTime());
            record.put("banExpireTime", user.getBanExpireTime());
            record.put("until", user.getBanExpireTime());
            record.put("unbanTime", user.getBanExpireTime());
            return record;
        }).<Object>map(record -> record).toList();
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        long total = records.size();
        long pages = Math.max(1, (total + safeSize - 1) / safeSize);
        int from = Math.min((safePage - 1) * safeSize, records.size());
        int to = Math.min(from + safeSize, records.size());
        return new PageData<>(records.subList(from, to), total, pages, safePage, safeSize);
    }

    private PageData<Object> toAdminUserPage(Page<User> userPage) {
        List<Object> records = userPage.getRecords().stream().map(user -> {
            Map<String, Object> record = new HashMap<>();
            record.put("id", user.getId());
            record.put("userId", user.getId());
            record.put("username", user.getUsername());
            record.put("user", user.getUsername());
            record.put("name", user.getUsername());
            record.put("role", user.getRole());
            record.put("userRole", user.getRole());
            record.put("createdAt", user.getCreateTime());
            record.put("createTime", user.getCreateTime());
            record.put("registerTime", user.getCreateTime());
            record.put("registeredAt", user.getCreateTime());
            record.put("registrationTime", user.getCreateTime());
            record.put("bannedUntil", user.getBanExpireTime());
            record.put("banExpireTime", user.getBanExpireTime());
            record.put("until", user.getBanExpireTime());
            record.put("unbanTime", user.getBanExpireTime());
            record.put("userInfo", user);
            return record;
        }).<Object>map(record -> record).toList();
        return new PageData<>(records, userPage.getTotal(), Math.max(1, userPage.getPages()), userPage.getCurrent(), userPage.getSize());
    }
}
