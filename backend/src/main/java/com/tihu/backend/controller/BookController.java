package com.tihu.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.Book;
import com.tihu.backend.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书相关接口
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    /**
     * 获取图书列表（分页）
     * GET /api/books?page=1&size=10
     */
    @GetMapping
    public Result getBooks(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Page<Book> result = bookService.getBooks(page, size);
        return Result.success(result);
    }

    /**
     * 按书名搜索
     * GET /api/books/search?keyword=xxx&page=1&size=10
     */
    @GetMapping("/search")
    public Result searchByTitle(@RequestParam String keyword, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Page<Book> result = bookService.searchByTitle(keyword, page, size);
        return Result.success(result);
    }

    /**
     * 按标签搜索（多标签AND）
     * GET /api/books/search-by-tags?tags=tag1,tag2&page=1&size=10
     */
    @GetMapping("/search-by-tags")
    public Result searchByTags(@RequestParam List<String> tags, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Page<Book> result = bookService.searchByTags(tags, page, size);
        return Result.success(result);
    }

    /**
     * 获取图书详情
     * GET /api/books/{id}
     */
    @GetMapping("/{id}")
    public Result getBookDetail(@PathVariable Long id) {
        Object detail = bookService.getBookDetail(id);
        return detail != null ? Result.success(detail) : Result.error(404, "图书不存在", null);
    }

    /**
     * 管理员接口：新增图书
     * POST /api/books
     */
    @PostMapping
    public Result createBook(@RequestBody Book book) {
        bookService.save(book);
        return Result.success(book);
    }

    /**
     * 管理员接口：修改图书
     * PUT /api/books/{id}
     */
    @PutMapping("/{id}")
    public Result updateBook(@PathVariable Long id, @RequestBody Book book) {
        book.setId(id);
        bookService.updateById(book);
        return Result.success();
    }

    /**
     * 管理员接口：删除图书（逻辑删除）
     * DELETE /api/books/{id}
     */
    @DeleteMapping("/{id}")
    public Result deleteBook(@PathVariable Long id) {
        Book book = bookService.getById(id);
        if (book != null) {
            book.setIsDeleted(1);
            bookService.updateById(book);
        }
        return Result.success();
    }
}

