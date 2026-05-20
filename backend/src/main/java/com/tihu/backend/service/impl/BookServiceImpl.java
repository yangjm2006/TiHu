package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.entity.Book;
import com.tihu.backend.mapper.BookMapper;
import com.tihu.backend.service.BookService;
import com.tihu.backend.service.RatingService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
    
    @Autowired
    private RatingService ratingService;

    @Override
    public Page<Book> getBooks(int pageNum, int pageSize) {
        return this.page(new Page<>(pageNum, pageSize), 
            new LambdaQueryWrapper<Book>().eq(Book::getIsDeleted, 0).orderByDesc(Book::getAvgRating));
    }

    @Override
    public Page<Book> searchByTitle(String keyword, int pageNum, int pageSize) {
        return this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Book>().eq(Book::getIsDeleted, 0).like(Book::getTitle, keyword).orderByDesc(Book::getAvgRating));
    }

    @Override
    public Page<Book> searchByTags(List<String> tags, int pageNum, int pageSize) {
        // 此处需要复杂SQL，实现多标签AND逻辑
        // 可通过BookTagMapper查询
        return new Page<>(pageNum, pageSize);
    }

    @Override
    public Object getBookDetail(Long bookId) {
        Book book = this.getById(bookId);
        if (book == null || book.getIsDeleted() == 1) {
            return null;
        }
        // 返回包含评分统计的详情对象
        Object ratingStats = ratingService.getRatingStats(bookId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("bookInfo", book);
        detail.put("ratings", ratingStats);
        return detail;
    }
}

