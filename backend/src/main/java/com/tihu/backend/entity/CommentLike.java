package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞/点踩实体
 * 3态互斥：赞 / 踩 / 无
 * likeType: 0=赞, 1=踩
 */
@Data
@TableName("`comment_like`")
public class CommentLike {
    private Long id;
    
    // 用户ID
    private Long userId;
    
    // 评论ID
    private Long commentId;
    
    // 点赞类型：0=赞, 1=踩
    private Integer likeType;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;
}

