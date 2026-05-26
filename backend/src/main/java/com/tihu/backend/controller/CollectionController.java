package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.PageData;
import com.tihu.backend.common.Result;
import com.tihu.backend.dto.CollectionBookDTO;
import com.tihu.backend.service.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏相关接口
 */
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    /**
     * 收藏图书
     * POST /api/collections?bookId=xxx
     */
    @PostMapping
    public Result collectBook(@RequestParam Long bookId) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        collectionService.collectBook(userId, bookId);
        return Result.success();
    }

    /**
     * 取消收藏
     * DELETE /api/collections?bookId=xxx
     */
    @DeleteMapping
    public Result uncollectBook(@RequestParam Long bookId) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        collectionService.uncollectBook(userId, bookId);
        return Result.success();
    }

    /**
     * 获取我的收藏列表
     * GET /api/collections?page=1&size=10
     */
    @GetMapping
    public Result getMyCollections(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<CollectionBookDTO> collections = collectionService.getUserCollections(userId, page, size);
        return Result.success(PageData.of(collections));
    }

    /**
     * 检查是否已收藏
     * GET /api/collections/check?bookId=xxx
     */
    @GetMapping("/check")
    public Result checkCollected(@RequestParam Long bookId) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        boolean collected = collectionService.isCollected(userId, bookId);
        return Result.success(collected);
    }
}

