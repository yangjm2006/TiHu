package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.entity.CommentLike;

/**
 * 评论点赞/点踩Service
 */
public interface CommentLikeService extends IService<CommentLike> {
    /**
     * 点赞评论
     */
    void likeComment(Long userId, Long commentId) throws Exception;
    
    /**
     * 点踩评论
     */
    void dislikeComment(Long userId, Long commentId) throws Exception;
    
    /**
     * 取消点赞/点踩
     */
    void cancelLike(Long userId, Long commentId) throws Exception;
    
    /**
     * 获取用户对评论的点赞状态（0=无，1=赞，2=踩）
     */
    Integer getLikeStatus(Long userId, Long commentId);
}

