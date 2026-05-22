package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.PageData;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.Comment;
import com.tihu.backend.service.CommentService;
import com.tihu.backend.service.CommentLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 评论相关接口
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;
    
    @Autowired
    private CommentLikeService commentLikeService;

    /**
     * 发表评论/回复
     * POST /api/comments
     */
    @PostMapping
    public Result createComment(@RequestParam Long bookId, @RequestParam String content, @RequestParam(required = false) Long parentCommentId) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Comment comment = commentService.createComment(userId, bookId, content, parentCommentId);
        return Result.success(comment);
    }

    /**
     * 获取图书的评论列表
     * GET /api/comments/book/{bookId}?page=1&size=10
     */
    @GetMapping("/book/{bookId}")
    public Result getComments(@PathVariable Long bookId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Page<Object> comments = commentService.getComments(bookId, page, size);
        return Result.success(PageData.of(comments));
    }

    /**
     * 撤回评论（用户）
     * DELETE /api/comments/{id}
     */
    @DeleteMapping("/{id}")
    public Result withdrawComment(@PathVariable Long id) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        commentService.withdrawComment(id, userId);
        return Result.success();
    }

    /**
     * 点赞评论
     * POST /api/comments/{id}/like
     */
    @PostMapping("/{id}/like")
    public Result likeComment(@PathVariable Long id) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        commentLikeService.likeComment(userId, id);
        return Result.success();
    }

    /**
     * 点踩评论
     * POST /api/comments/{id}/dislike
     */
    @PostMapping("/{id}/dislike")
    public Result dislikeComment(@PathVariable Long id) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        commentLikeService.dislikeComment(userId, id);
        return Result.success();
    }

    /**
     * 取消点赞/踩
     * DELETE /api/comments/{id}/like
     */
    @DeleteMapping("/{id}/like")
    public Result cancelLike(@PathVariable Long id) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        commentLikeService.cancelLike(userId, id);
        return Result.success();
    }

    /**
     * 管理员接口：删除任意评论/回复
     * DELETE /api/comments/admin/{id}
     */
    @DeleteMapping("/admin/{id}")
    public Result deleteComment(@PathVariable Long id) throws Exception {
        commentService.deleteComment(id);
        return Result.success();
    }

    /**
     * 管理员接口：查看全站评论
     * GET /api/comments/admin/all
     */
    @GetMapping("/admin/all")
    public Result getAllComments() throws Exception {
        Object comments = commentService.getAllComments();
        return Result.success(comments);
    }
}

