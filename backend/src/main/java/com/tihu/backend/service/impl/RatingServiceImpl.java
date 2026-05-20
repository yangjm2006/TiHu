package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.Rating;
import com.tihu.backend.mapper.RatingMapper;
import com.tihu.backend.service.RatingService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RatingServiceImpl extends ServiceImpl<RatingMapper, Rating> implements RatingService {

    @Override
    public Rating submitRating(Long userId, Long bookId, Integer score) throws Exception {
        if (score < 1 || score > 10) {
            throw new ApiException(400, "评分必须在1-10之间");
        }
        
        Rating existing = this.getOne(new LambdaQueryWrapper<Rating>().eq(Rating::getUserId, userId).eq(Rating::getBookId, bookId));
        if (existing != null) {
            existing.setScore(score);
            this.updateById(existing);
            return existing;
        }
        
        Rating rating = new Rating();
        rating.setUserId(userId);
        rating.setBookId(bookId);
        rating.setScore(score);
        this.save(rating);
        return rating;
    }

    @Override
    public Rating getUserRating(Long userId, Long bookId) {
        return this.getOne(new LambdaQueryWrapper<Rating>().eq(Rating::getUserId, userId).eq(Rating::getBookId, bookId));
    }

    @Override
    public Map<String, Object> getRatingStats(Long bookId) {
        List<Integer> scores = this.list(new LambdaQueryWrapper<Rating>().eq(Rating::getBookId, bookId))
            .stream().map(Rating::getScore).toList();
        
        double avgRating = scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            final int score = i;
            distribution.put(i, scores.stream().filter(s -> s == score).count());
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("avgScore", avgRating);
        stats.put("ratingCount", scores.size());
        stats.put("distribution", distribution);
        return stats;
    }
}

