package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论/回复实体（两级结构）
 * parentCommentId为NULL时是一级评论
 * parentCommentId不为NULL时是二级回复
 */
@Data
@TableName("`comment`")
public class Comment {
    private Long id;
    
    // 评论所属用户ID
    private Long userId;
    
    // 评论所属图书ID
    private Long bookId;
    
    // 评论内容（最多200字）
    private String content;
    
    // 父评论ID（NULL表示一级评论）
    private Long parentCommentId;
    
    // 点赞数
    private Integer likeCount;
    
    // 点踩数
    private Integer dislikeCount;
    
    // 逻辑删除/撤回标识
    @TableLogic
    private Integer isDeleted;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;
}

