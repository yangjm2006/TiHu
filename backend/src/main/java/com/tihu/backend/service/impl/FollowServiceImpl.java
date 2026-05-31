package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.Follow;
import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.FollowMapper;
import com.tihu.backend.mapper.UserMapper;
import com.tihu.backend.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void followUser(Long followerId, Long followeeId) throws Exception {
        if (followerId.equals(followeeId)) {
            throw new ApiException(400, "不能关注自己");
        }
        if (userMapper.selectById(followeeId) == null) {
            throw new ApiException(404, "用户不存在");
        }
        
        Follow existing = this.getOne(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, followerId).eq(Follow::getFolloweeId, followeeId));
        if (existing != null) {
            return;
        }
        
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        this.save(follow);
    }

    @Override
    public void unfollowUser(Long followerId, Long followeeId) throws Exception {
        this.remove(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, followerId).eq(Follow::getFolloweeId, followeeId));
    }

    @Override
    public Page<Object> getFollowees(Long userId, int pageNum, int pageSize) {
        Page<Follow> page = this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId).orderByDesc(Follow::getCreateTime));
        return toUserPage(page, true);
    }

    @Override
    public Page<Object> getFollowers(Long userId, int pageNum, int pageSize) {
        Page<Follow> page = this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Follow>().eq(Follow::getFolloweeId, userId).orderByDesc(Follow::getCreateTime));
        return toUserPage(page, false);
    }

    @Override
    public long getFolloweeCount(Long userId) {
        return this.count(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId));
    }

    @Override
    public long getFollowerCount(Long userId) {
        return this.count(new LambdaQueryWrapper<Follow>().eq(Follow::getFolloweeId, userId));
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return this.getOne(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, followerId).eq(Follow::getFolloweeId, followeeId)) != null;
    }

    private Page<Object> toUserPage(Page<Follow> followPage, boolean followeeList) {
        Page<Object> result = new Page<>(followPage.getCurrent(), followPage.getSize(), followPage.getTotal());
        result.setPages(followPage.getPages());
        result.setRecords(followPage.getRecords().stream()
            .map(follow -> toRecord(follow, followeeList))
            .<Object>map(record -> record)
            .toList());
        return result;
    }

    private Map<String, Object> toRecord(Follow follow, boolean followeeList) {
        Long userId = followeeList ? follow.getFolloweeId() : follow.getFollowerId();
        User user = userMapper.selectById(userId);
        Map<String, Object> record = new HashMap<>();
        record.put(followeeList ? "followee" : "follower", user);
        record.put("userInfo", user);
        record.put("username", user != null ? user.getUsername() : null);
        record.put("followeeId", follow.getFolloweeId());
        record.put("followerId", follow.getFollowerId());
        record.put("createTime", follow.getCreateTime());
        return record;
    }
}

