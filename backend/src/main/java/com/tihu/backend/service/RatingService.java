package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.entity.Rating;

import java.util.Map;

/**
 * 评分Service
 */
public interface RatingService extends IService<Rating> {
    /**
     * 提交/更新评分
     */
    Rating submitRating(Long userId, Long bookId, Integer score) throws Exception;
    
    /**
     * 获取用户对书籍的评分
     */
    Rating getUserRating(Long userId, Long bookId);
    
    /**
     * 获取书籍评分统计
     */
    Map<String, Object> getRatingStats(Long bookId);
}

