package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.PageData;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.User;
import com.tihu.backend.service.FollowService;
import com.tihu.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 关注相关接口
 */
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserService userService;

    /**
     * 关注用户
     * POST /api/follows?followeeId=xxx 或 /api/follows?followeeUsername=xxx
     */
    @PostMapping
    public Result followUser(@RequestParam(required = false) Long followeeId,
                             @RequestParam(required = false) String followeeUsername) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        followService.followUser(userId, resolveFolloweeId(followeeId, followeeUsername));
        return Result.success();
    }

    /**
     * 取消关注
     * DELETE /api/follows?followeeId=xxx 或 /api/follows?followeeUsername=xxx
     */
    @DeleteMapping
    public Result unfollowUser(@RequestParam(required = false) Long followeeId,
                               @RequestParam(required = false) String followeeUsername) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        followService.unfollowUser(userId, resolveFolloweeId(followeeId, followeeUsername));
        return Result.success();
    }

    /**
     * 获取我的关注列表
     * GET /api/follows/followees?page=1&size=10
     */
    @GetMapping("/followees")
    public Result getFollowees(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "1000") int size) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<Object> result = followService.getFollowees(userId, page, size);
        return Result.success(PageData.of(result));
    }

    /**
     * 获取我的粉丝列表
     * GET /api/follows/followers?page=1&size=10
     */
    @GetMapping("/followers")
    public Result getFollowers(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "1000") int size) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<Object> result = followService.getFollowers(userId, page, size);
        return Result.success(PageData.of(result));
    }

    /**
     * 获取某用户的关注列表
     * GET /api/follows/user/{userId}/followees
     */
    @GetMapping("/user/{userId}/followees")
    public Result getUserFollowees(@PathVariable Long userId,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "1000") int size) {
        Page<Object> result = followService.getFollowees(userId, page, size);
        return Result.success(PageData.of(result));
    }

    /**
     * 获取某用户的粉丝列表
     * GET /api/follows/user/{userId}/followers
     */
    @GetMapping("/user/{userId}/followers")
    public Result getUserFollowers(@PathVariable Long userId,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "1000") int size) {
        Page<Object> result = followService.getFollowers(userId, page, size);
        return Result.success(PageData.of(result));
    }

    /**
     * 检查是否关注了某用户
     * GET /api/follows/check?followeeId=xxx
     */
    @GetMapping("/check")
    public Result checkFollowing(@RequestParam(required = false) Long followeeId,
                                 @RequestParam(required = false) String followeeUsername) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        boolean following = followService.isFollowing(userId, resolveFolloweeId(followeeId, followeeUsername));
        return Result.success(following);
    }

    private Long resolveFolloweeId(Long followeeId, String followeeUsername) {
        if (followeeId != null) {
            return followeeId;
        }
        if (followeeUsername == null || followeeUsername.isBlank()) {
            throw new ApiException(400, "followeeId或followeeUsername不能为空");
        }
        User user = userService.getUserByUsername(followeeUsername.trim());
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        return user.getId();
    }
}

