package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.PageData;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.BookList;
import com.tihu.backend.service.BookListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 书单相关接口
 */
@RestController
@RequestMapping("/api/book-lists")
public class BookListController {

    @Autowired
    private BookListService bookListService;

    /**
     * 创建书单
     * POST /api/book-lists
     */
    @PostMapping
    public Result createBookList(@RequestParam String title, @RequestParam(required = false) String description) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        BookList bookList = bookListService.createBookList(userId, title, description);
        return Result.success(bookList);
    }

    /**
     * 获取我的书单列表
     * GET /api/book-lists?page=1&size=10
     */
    @GetMapping
    public Result getMyBookLists(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<BookList> result = bookListService.getUserBookLists(userId, page, size);
        return Result.success(PageData.of(result));
    }

    /**
     * 获取书单详情
     * GET /api/book-lists/{id}
     */
    @GetMapping("/{id}")
    public Result getBookListDetail(@PathVariable Long id) {
        Object detail = bookListService.getBookListDetail(id);
        return Result.success(detail);
    }

    /**
     * 向书单添加书籍
     * POST /api/book-lists/{id}/books?bookId=xxx
     */
    @PostMapping("/{id}/books")
    public Result addBookToList(@PathVariable Long id, @RequestParam Long bookId) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        bookListService.addBookToList(id, bookId, userId);
        return Result.success();
    }

    /**
     * 从书单移除书籍
     * DELETE /api/book-lists/{id}/books?bookId=xxx
     */
    @DeleteMapping("/{id}/books")
    public Result removeBookFromList(@PathVariable Long id, @RequestParam Long bookId) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        bookListService.removeBookFromList(id, bookId, userId);
        return Result.success();
    }

    /**
     * 删除书单
     * DELETE /api/book-lists/{id}
     */
    @DeleteMapping("/{id}")
    public Result deleteBookList(@PathVariable Long id) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        bookListService.deleteBookList(id, userId);
        return Result.success();
    }
}

