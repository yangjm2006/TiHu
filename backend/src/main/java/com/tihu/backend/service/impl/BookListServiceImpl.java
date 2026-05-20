package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.BookList;
import com.tihu.backend.mapper.BookListMapper;
import com.tihu.backend.service.BookListService;
import org.springframework.stereotype.Service;

@Service
public class BookListServiceImpl extends ServiceImpl<BookListMapper, BookList> implements BookListService {

    @Override
    public BookList createBookList(Long userId, String title, String description) throws Exception {
        BookList bookList = new BookList();
        bookList.setUserId(userId);
        bookList.setTitle(title);
        bookList.setDescription(description);
        bookList.setIsPublic(true);
        this.save(bookList);
        return bookList;
    }

    @Override
    public Page<BookList> getUserBookLists(Long userId, int pageNum, int pageSize) {
        return this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<BookList>().eq(BookList::getUserId, userId).orderByDesc(BookList::getCreateTime));
    }

    @Override
    public Object getBookListDetail(Long bookListId) {
        // 返回书单及其书籍列表
        return null;
    }

    @Override
    public void addBookToList(Long bookListId, Long bookId, Long userId) throws Exception {
        BookList bookList = this.getById(bookListId);
        if (bookList == null) {
            throw new ApiException(404, "书单不存在");
        }
        if (!bookList.getUserId().equals(userId)) {
            throw new ApiException(403, "无权修改他人书单");
        }
    }

    @Override
    public void removeBookFromList(Long bookListId, Long bookId, Long userId) throws Exception {
        BookList bookList = this.getById(bookListId);
        if (bookList == null) {
            throw new ApiException(404, "书单不存在");
        }
        if (!bookList.getUserId().equals(userId)) {
            throw new ApiException(403, "无权修改他人书单");
        }
    }

    @Override
    public void deleteBookList(Long bookListId, Long userId) throws Exception {
        BookList bookList = this.getById(bookListId);
        if (bookList == null) {
            throw new ApiException(404, "书单不存在");
        }
        if (!bookList.getUserId().equals(userId)) {
            throw new ApiException(403, "无权删除他人书单");
        }
        this.removeById(bookListId);
    }
}

