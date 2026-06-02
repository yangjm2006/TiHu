package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.dto.CollectionBookDTO;
import com.tihu.backend.entity.Book;
import com.tihu.backend.entity.Collection;
import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.CollectionMapper;
import com.tihu.backend.service.CollectionService;
import com.tihu.backend.service.BookService;
import com.tihu.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Service
public class CollectionServiceImpl extends ServiceImpl<CollectionMapper, Collection> implements CollectionService {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Override
    public void collectBook(Long userId, Long bookId) {
        requireExistingBook(bookId);

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
    public void uncollectBook(Long userId, Long bookId) {
        this.remove(new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId).eq(Collection::getBookId, bookId));
    }

    @Override
    public Page<CollectionBookDTO> getUserCollections(Long userId, int pageNum, int pageSize) {
        int safePageSize = pageSize <= 0 ? 10 : pageSize;
        Page<Collection> page = this.page(
                new Page<>(pageNum, safePageSize),
                new LambdaQueryWrapper<Collection>()
                        .eq(Collection::getUserId, userId)
                        .orderByDesc(Collection::getCreateTime)
        );

        List<CollectionBookDTO> records = new ArrayList<>();
        User user = userService.getById(userId);
        String username = user == null ? null : user.getUsername();

        for (Collection collection : page.getRecords()) {
            Object bookDetail = bookService.getBookDetail(collection.getBookId());
            if (bookDetail == null) {
                continue;
            }

            CollectionBookDTO dto = new CollectionBookDTO();
            dto.setId(collection.getBookId());
            dto.setBookId(collection.getBookId());
            dto.setOwner(username);
            dto.setUsername(username);
            dto.setCreateTime(collection.getCreateTime());

            if (bookDetail instanceof java.util.Map<?, ?> detailMap) {
                Object bookInfo = detailMap.get("bookInfo");
                Object ratings = detailMap.get("ratings");
                dto.setBookInfo(bookInfo);
                dto.setRatings(ratings);
                if (bookInfo instanceof Book book) {
                    dto.setTitle(book.getTitle());
                    dto.setAuthor(book.getAuthor());
                    dto.setDescription(book.getDescription());
                    dto.setTags(book.getTags());
                    dto.setAverageScore(book.getAvgRating());
                }
                if (ratings instanceof java.util.Map<?, ?> ratingMap) {
                    Object avgScore = ratingMap.get("avgScore");
                    Object ratingCount = ratingMap.get("ratingCount");
                    if (avgScore instanceof Number number) {
                        dto.setAvgScore(number.doubleValue());
                    }
                    if (ratingCount instanceof Number number) {
                        dto.setRatingCount(number.intValue());
                    }
                }
            } else {
                dto.setBookInfo(bookDetail);
            }
            records.add(dto);
        }

        Page<CollectionBookDTO> result = new Page<>(pageNum, safePageSize);
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setPages((page.getTotal() + safePageSize - 1L) / safePageSize);
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    @Override
    public boolean isCollected(Long userId, Long bookId) {
        return this.getOne(new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId).eq(Collection::getBookId, bookId)) != null;
    }

    private void requireExistingBook(Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null || Integer.valueOf(1).equals(book.getIsDeleted())) {
            throw new ApiException(404, "图书不存在");
        }
    }
}

