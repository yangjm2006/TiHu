package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.entity.Book;
import com.tihu.backend.entity.Tag;

import java.util.List;

/**
 * 图书Service
 */
public interface BookService extends IService<Book> {
    /**
     * 分页查询图书
     */
    Page<Book> getBooks(int pageNum, int pageSize, String sort);

    /**
     * 按书名搜索
     */
    Page<Book> searchByTitle(String keyword, int pageNum, int pageSize, String sort);

    /**
     * 按标签搜索（多标签AND）
     */
    Page<Book> searchByTags(List<String> tags, int pageNum, int pageSize, String sort);

    /**
     * 获取图书标签
     */
    List<Tag> getBookTags(Long bookId);

    /**
     * 替换图书标签（按标签名保存）
     */
    void replaceBookTags(Long bookId, List<String> tagNames);

    /**
     * 获取图书详情（含评分统计）
     */
    Object getBookDetail(Long bookId);
}
