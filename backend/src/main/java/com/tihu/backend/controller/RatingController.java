package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.Rating;
import com.tihu.backend.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评分相关接口
 */
@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    /**
     * 提交/更新评分
     * POST /api/ratings?bookId=xxx&score=8
     */
    @PostMapping
    public Result submitRating(@RequestParam Long bookId, @RequestParam Integer score) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Rating rating = ratingService.submitRating(userId, bookId, score);
        return Result.success(rating);
    }

    /**
     * 获取我的评分
     * GET /api/ratings/my?bookId=xxx
     */
    @GetMapping("/my")
    public Result getMyRating(@RequestParam Long bookId) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Rating rating = ratingService.getUserRating(userId, bookId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("score", rating == null ? null : rating.getScore());
        data.put("myScore", rating == null ? null : rating.getScore());
        data.put("rating", rating);
        return Result.success(data);
    }

    /**
     * 获取图书评分统计
     * GET /api/ratings/book/{bookId}/stats
     */
    @GetMapping("/book/{bookId}/stats")
    public Result getRatingStats(@PathVariable Long bookId) {
        Map<String, Object> stats = ratingService.getRatingStats(bookId);
        return Result.success(stats);
    }
}

