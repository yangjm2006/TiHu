package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.PageData;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.Book;
import com.tihu.backend.dto.BookTagsRequest;
import com.tihu.backend.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

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
     * GET /api/books?page=1&size=10&sort=default
     */
    @GetMapping
    public Result getBooks(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(defaultValue = "default") String sort) {
        Page<Book> result = bookService.getBooks(page, size, sort);
        return Result.success(PageData.of(result));
    }

    /**
     * 按书名搜索
     * GET /api/books/search?keyword=xxx&page=1&size=10
     */
    @GetMapping("/search")
    public Result searchByTitle(@RequestParam String keyword,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(defaultValue = "default") String sort) {
        Page<Book> result = bookService.searchByTitle(keyword, page, size, sort);
        return Result.success(PageData.of(result));
    }

    /**
     * 按标签搜索（多标签AND）
     * GET /api/books/search-by-tags?tags=tag1,tag2&page=1&size=10&sort=default
     */
    @GetMapping("/search-by-tags")
    public Result searchByTags(@RequestParam List<String> tags,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "default") String sort) {
        Page<Book> result = bookService.searchByTags(tags, page, size, sort);
        return Result.success(PageData.of(result));
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
    @Transactional(rollbackFor = Exception.class)
    public Result createBook(@RequestBody Book book) {
        requireTitle(book);
        normalizeTitle(book);
        if (bookService.lambdaQuery().eq(Book::getTitle, book.getTitle()).count() > 0) {
            throw new ApiException(409, "书名已存在");
        }
        bookService.save(book);
        if (book.getTags() != null) {
            bookService.replaceBookTags(book.getId(), book.getTags());
        }
        return Result.success(book);
    }

    /**
     * 管理员接口：修改图书
     * PUT /api/books/{id}
     */
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result updateBook(@PathVariable Long id, @RequestBody Book book) {
        book.setId(id);
        requireTitle(book);
        normalizeTitle(book);
        if (bookService.lambdaQuery().eq(Book::getTitle, book.getTitle()).ne(Book::getId, id).count() > 0) {
            throw new ApiException(409, "书名已存在");
        }
        bookService.updateById(book);
        if (book.getTags() != null) {
            bookService.replaceBookTags(book.getId(), book.getTags());
        }
        return Result.success();
    }

    /**
     * 获取图书标签
     * GET /api/books/{id}/tags
     */
    @GetMapping("/{id}/tags")
    public Result getBookTags(@PathVariable Long id) {
        return Result.success(bookService.getBookTags(id));
    }

    /**
     * 替换图书标签（管理员）
     * PUT /api/books/{id}/tags
     */
    @PutMapping("/{id}/tags")
    public Result replaceBookTags(@PathVariable Long id, @RequestBody BookTagsRequest request) {
        StpUtil.checkRole("ADMIN");
        bookService.replaceBookTags(id, request == null ? null : request.getTags());
        return Result.success(bookService.getBookTags(id));
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

    private void normalizeTitle(Book book) {
        if (book != null && book.getTitle() != null) {
            book.setTitle(book.getTitle().trim());
        }
    }

    private void requireTitle(Book book) {
        if (book == null || book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new ApiException(400, "书名不能为空");
        }
    }
}

