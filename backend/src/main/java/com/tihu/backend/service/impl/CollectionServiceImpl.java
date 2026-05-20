package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.Collection;
import com.tihu.backend.mapper.CollectionMapper;
import com.tihu.backend.service.CollectionService;
import org.springframework.stereotype.Service;

@Service
public class CollectionServiceImpl extends ServiceImpl<CollectionMapper, Collection> implements CollectionService {

    @Override
    public void collectBook(Long userId, Long bookId) throws Exception {
        Collection existing = this.getOne(new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId).eq(Collection::getBookId, bookId));
        if (existing != null) {
            throw new ApiException(409, "已收藏过该图书");
        }
        
        Collection collection = new Collection();
        collection.setUserId(userId);
        collection.setBookId(bookId);
        this.save(collection);
    }

    @Override
    public void uncollectBook(Long userId, Long bookId) throws Exception {
        this.remove(new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId).eq(Collection::getBookId, bookId));
    }

    @Override
    public Page<Object> getUserCollections(Long userId, int pageNum, int pageSize) {
        Page<Collection> page = this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId));
        return (Page<Object>) (Page<?>) page;
    }

    @Override
    public boolean isCollected(Long userId, Long bookId) {
        return this.getOne(new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId).eq(Collection::getBookId, bookId)) != null;
    }
}

