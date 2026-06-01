package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.Constants;
import com.tihu.backend.entity.Comment;
import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.CommentMapper;
import com.tihu.backend.mapper.UserMapper;
import com.tihu.backend.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public Object createComment(Long userId, Long bookId, String content, Long parentCommentId) throws Exception {
        if (content == null || content.isBlank() || content.length() > Constants.COMMENT_MAX_LENGTH) {
            throw new ApiException(400, "评论内容不能为空，最多200字");
        }
        if (parentCommentId != null) {
            Comment parent = this.getById(parentCommentId);
            if (parent == null || Integer.valueOf(1).equals(parent.getIsDeleted())) {
                throw new ApiException(404, "父评论不存在");
            }
            if (parent.getParentCommentId() != null) {
                throw new ApiException(400, "只能回复一级评论");
            }
            if (!parent.getBookId().equals(bookId)) {
                throw new ApiException(400, "父评论不属于该图书");
            }
        }
        
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setBookId(bookId);
        comment.setContent(content.trim());
        comment.setParentCommentId(parentCommentId);
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        comment.setIsDeleted(0);
        
        this.save(comment);

        Map<String, Object> data = new HashMap<>();
        data.put("comment", toCommentRecord(comment));
        return data;
    }

    @Override
    public Page<Object> getComments(Long bookId, int pageNum, int pageSize) {
        // 返回一级评论和回复，前端通过 parentId 组织两级结构。
        Page<Comment> page = this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getBookId, bookId)
                .eq(Comment::getIsDeleted, 0)
                .orderByAsc(Comment::getCreateTime));

        Page<Object> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords().stream()
            .map(this::toCommentRecord)
            .<Object>map(record -> record)
            .toList());
        return result;
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
        
        deleteWithReplies(comment);
    }

    @Override
    public void deleteComment(Long commentId) throws Exception {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new ApiException(404, "评论不存在");
        }
        
        deleteWithReplies(comment);
    }

    /**
     * 获取全站所有评论（管理员）
     */
    @Override
    public Page<Object> getAllComments(int pageNum, int pageSize) throws Exception {
        Page<Comment> page = this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getIsDeleted, 0)
                .orderByDesc(Comment::getCreateTime));

        Page<Object> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords().stream()
            .map(this::toCommentRecord)
            .<Object>map(record -> record)
            .toList());
        return result;
    }

    private void deleteWithReplies(Comment comment) {
        if (comment.getParentCommentId() == null) {
            LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Comment::getParentCommentId, comment.getId())
                .eq(Comment::getIsDeleted, 0);
            this.remove(wrapper);
        }

        boolean removed = this.removeById(comment.getId());
        if (!removed) {
            throw new ApiException(404, "评论不存在或已删除");
        }
    }

    private Map<String, Object> toCommentRecord(Comment comment) {
        User user = userMapper.selectById(comment.getUserId());
        Integer likeCount = comment.getLikeCount() == null ? 0 : comment.getLikeCount();
        Integer dislikeCount = comment.getDislikeCount() == null ? 0 : comment.getDislikeCount();

        Map<String, Object> record = new HashMap<>();
        record.put("id", comment.getId());
        record.put("commentId", comment.getId());
        record.put("userId", comment.getUserId());
        record.put("username", user != null ? user.getUsername() : null);
        record.put("user", user != null ? user.getUsername() : null);
        record.put("nickname", user != null ? user.getUsername() : null);
        record.put("bookId", comment.getBookId());
        record.put("content", comment.getContent());
        record.put("text", comment.getContent());
        record.put("createTime", comment.getCreateTime());
        record.put("time", comment.getCreateTime());
        record.put("createdAt", comment.getCreateTime());
        record.put("parentId", comment.getParentCommentId());
        record.put("parentCommentId", comment.getParentCommentId());
        record.put("replyTo", comment.getParentCommentId());
        record.put("upVotes", likeCount);
        record.put("upvoteCount", likeCount);
        record.put("likes", likeCount);
        record.put("likeCount", likeCount);
        record.put("downVotes", dislikeCount);
        record.put("downvoteCount", dislikeCount);
        record.put("dislikes", dislikeCount);
        record.put("dislikeCount", dislikeCount);
        return record;
    }
}

