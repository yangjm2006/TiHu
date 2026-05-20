package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.Constants;
import com.tihu.backend.entity.Comment;
import com.tihu.backend.mapper.CommentMapper;
import com.tihu.backend.service.CommentService;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Override
    public Comment createComment(Long userId, Long bookId, String content, Long parentCommentId) throws Exception {
        if (content == null || content.isEmpty() || content.length() > Constants.COMMENT_MAX_LENGTH) {
            throw new ApiException(400, "评论内容不能为空，最多200字");
        }
        
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setBookId(bookId);
        comment.setContent(content);
        comment.setParentCommentId(parentCommentId);
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        comment.setIsDeleted(0);
        
        this.save(comment);
        return comment;
    }

    @Override
    public Page<Object> getComments(Long bookId, int pageNum, int pageSize) {
        // 查询一级评论及其回复
        Page<Comment> page = this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getBookId, bookId)
                .isNull(Comment::getParentCommentId)
                .eq(Comment::getIsDeleted, 0)
                .orderByDesc(Comment::getCreateTime));
        
        return new Page<>(pageNum, pageSize);
    }

    @Override
    public void withdrawComment(Long commentId, Long userId) throws Exception {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new ApiException(404, "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new ApiException(403, "只能撤回自己的评论");
        }
        
        comment.setIsDeleted(1);
        this.updateById(comment);
    }

    @Override
    public void deleteComment(Long commentId) throws Exception {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new ApiException(404, "评论不存在");
        }
        
        comment.setIsDeleted(1);
        this.updateById(comment);
    }
}

