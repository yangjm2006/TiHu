package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 书单实体（全部公开）
 */
@Data
@TableName("`book_list`")
public class BookList {
    private Long id;
    
    // 书单所有者ID
    private Long userId;
    
    // 书单标题
    private String title;
    
    // 书单简介
    private String description;
    
    // 书单封面（可选）
    private String cover;
    
    // 是否公开（V1全部公开，预留字段）
    private Boolean isPublic;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;
}

