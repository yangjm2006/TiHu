package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.Comment;
import com.tihu.backend.entity.CommentLike;
import com.tihu.backend.mapper.CommentMapper;
import com.tihu.backend.mapper.CommentLikeMapper;
import com.tihu.backend.service.CommentLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike> implements CommentLikeService {

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public void likeComment(Long userId, Long commentId) throws Exception {
        CommentLike existing = this.getOne(new LambdaQueryWrapper<CommentLike>().eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, commentId));
        
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new ApiException(404, "评论不存在");
        }
        
        if (existing != null) {
            if (existing.getLikeType() == 0) {
                return; // 已经点赞
            }
            // 切换：踩 -> 赞
            this.removeById(existing.getId());
            comment.setDislikeCount(Math.max(0, count(comment.getDislikeCount()) - 1));
        }
        
        CommentLike like = new CommentLike();
        like.setUserId(userId);
        like.setCommentId(commentId);
        like.setLikeType(0);
        this.save(like);
        
        comment.setLikeCount(count(comment.getLikeCount()) + 1);
        commentMapper.updateById(comment);
    }

    @Override
    public void dislikeComment(Long userId, Long commentId) throws Exception {
        CommentLike existing = this.getOne(new LambdaQueryWrapper<CommentLike>().eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, commentId));
        
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new ApiException(404, "评论不存在");
        }
        
        if (existing != null) {
            if (existing.getLikeType() == 1) {
                return; // 已经点踩
            }
            // 切换：赞 -> 踩
            this.removeById(existing.getId());
            comment.setLikeCount(Math.max(0, count(comment.getLikeCount()) - 1));
        }
        
        CommentLike like = new CommentLike();
        like.setUserId(userId);
        like.setCommentId(commentId);
        like.setLikeType(1);
        this.save(like);
        
        comment.setDislikeCount(count(comment.getDislikeCount()) + 1);
        commentMapper.updateById(comment);
    }

    @Override
    public void cancelLike(Long userId, Long commentId) throws Exception {
        CommentLike existing = this.getOne(new LambdaQueryWrapper<CommentLike>().eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, commentId));
        
        if (existing != null) {
            Comment comment = commentMapper.selectById(commentId);
            if (comment == null) {
                this.removeById(existing.getId());
                return;
            }
            if (existing.getLikeType() == 0) {
                comment.setLikeCount(Math.max(0, count(comment.getLikeCount()) - 1));
            } else {
                comment.setDislikeCount(Math.max(0, count(comment.getDislikeCount()) - 1));
            }
            commentMapper.updateById(comment);
            this.removeById(existing.getId());
        }
    }

    @Override
    public Integer getLikeStatus(Long userId, Long commentId) {
        CommentLike like = this.getOne(new LambdaQueryWrapper<CommentLike>().eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, commentId));
        if (like == null) {
            return 0; // 无
        }
        return like.getLikeType() == 0 ? 1 : 2; // 1=赞, 2=踩
    }

    private int count(Integer value) {
        return value == null ? 0 : value;
    }
}

