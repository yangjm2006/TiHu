package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.entity.Comment;

/**
 * 评论Service
 */
public interface CommentService extends IService<Comment> {
    /**
     * 创建评论/回复
     */
    Object createComment(Long userId, Long bookId, String content, Long parentCommentId) throws Exception;
    
    /**
     * 获取图书的评论列表（含二级回复）
     */
    Page<Object> getComments(Long bookId, int pageNum, int pageSize);
    
    /**
     * 撤回评论（普通用户）
     */
    void withdrawComment(Long commentId, Long userId) throws Exception;
    
    /**
     * 删除评论（管理员）
     */
    void deleteComment(Long commentId) throws Exception;

    /**
     * 获取全站所有评论（管理员）
     */
    Page<Object> getAllComments(int pageNum, int pageSize) throws Exception;
}

