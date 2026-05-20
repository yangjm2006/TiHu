package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.Follow;
import com.tihu.backend.mapper.FollowMapper;
import com.tihu.backend.service.FollowService;
import org.springframework.stereotype.Service;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Override
    public void followUser(Long followerId, Long followeeId) throws Exception {
        if (followerId.equals(followeeId)) {
            throw new ApiException(400, "不能关注自己");
        }
        
        Follow existing = this.getOne(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, followerId).eq(Follow::getFolloweeId, followeeId));
        if (existing != null) {
            throw new ApiException(409, "已关注该用户");
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
        return (Page<Object>) (Page<?>) page;
    }

    @Override
    public Page<Object> getFollowers(Long userId, int pageNum, int pageSize) {
        Page<Follow> page = this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Follow>().eq(Follow::getFolloweeId, userId).orderByDesc(Follow::getCreateTime));
        return (Page<Object>) (Page<?>) page;
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
}

