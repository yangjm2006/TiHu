package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系实体（单向）
 */
@Data
@TableName("`follow`")
public class Follow {
    private Long id;
    
    // 粉丝ID（追随者）
    private Long followerId;
    
    // 被关注用户ID（被追随者）
    private Long followeeId;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;
}

