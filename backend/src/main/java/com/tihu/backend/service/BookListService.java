package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.entity.BookList;

/**
 * 书单Service
 */
public interface BookListService extends IService<BookList> {
    /**
     * 创建书单
     */
    Object createBookList(Long userId, String title, String description, Boolean publicVisible, String visibility) throws Exception;
    
    /**
     * 获取用户的书单列表
     */
    Page<Object> getUserBookLists(Long userId, int pageNum, int pageSize);

    /**
     * 获取当前用户可见的书单列表：自己的全部书单 + 他人的公开书单
     */
    Page<Object> getVisibleBookLists(Long viewerUserId, int pageNum, int pageSize);
    
    /**
     * 获取书单详情（含书籍列表）
     */
    Object getBookListDetail(Long bookListId, Long currentUserId);

    /**
     * 修改书单可见性
     */
    void updateVisibility(Long bookListId, Long userId, Boolean publicVisible, String visibility) throws Exception;
    
    /**
     * 添加书到书单
     */
    void addBookToList(Long bookListId, Long bookId, Long userId) throws Exception;

    /**
     * 按书名添加书到书单
     */
    void addBookToListByTitle(Long bookListId, String bookTitle, Long userId) throws Exception;
    
    /**
     * 从书单移除书
     */
    void removeBookFromList(Long bookListId, Long bookId, Long userId) throws Exception;
    
    /**
     * 删除书单
     */
    void deleteBookList(Long bookListId, Long userId) throws Exception;

    /**
     * 获取用户主页可见书单
     */
    Object getProfileBookLists(Long profileUserId, Long viewerUserId);
}

