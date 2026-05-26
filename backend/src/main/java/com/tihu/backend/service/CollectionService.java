package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.dto.CollectionBookDTO;
import com.tihu.backend.entity.Collection;

/**
 * 收藏Service
 */
public interface CollectionService extends IService<Collection> {
    /**
     * 收藏图书
     */
    void collectBook(Long userId, Long bookId);

    /**
     * 取消收藏
     */
    void uncollectBook(Long userId, Long bookId);

    /**
     * 获取用户收藏列表
     */
    Page<CollectionBookDTO> getUserCollections(Long userId, int pageNum, int pageSize);

    /**
     * 检查用户是否已收藏
     */
    boolean isCollected(Long userId, Long bookId);
}

