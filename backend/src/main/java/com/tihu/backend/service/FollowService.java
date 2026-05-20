package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.entity.Follow;

/**
 * 关注Service
 */
public interface FollowService extends IService<Follow> {
    /**
     * 关注用户
     */
    void followUser(Long followerId, Long followeeId) throws Exception;
    
    /**
     * 取消关注
     */
    void unfollowUser(Long followerId, Long followeeId) throws Exception;
    
    /**
     * 获取关注列表
     */
    Page<Object> getFollowees(Long userId, int pageNum, int pageSize);
    
    /**
     * 获取粉丝列表
     */
    Page<Object> getFollowers(Long userId, int pageNum, int pageSize);
    
    /**
     * 获取关注数
     */
    long getFolloweeCount(Long userId);
    
    /**
     * 获取粉丝数
     */
    long getFollowerCount(Long userId);
    
    /**
     * 是否已关注
     */
    boolean isFollowing(Long followerId, Long followeeId);
}

