package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书实体
 */
@Data
@TableName("`book`")
public class Book {
    private Long id;
    
    // 书名（唯一）
    private String title;
    
    // 作者
    private String author;
    
    // ISBN
    private String isbn;
    
    // 简介
    private String description;
    
    // 封面URL（V1统一使用默认封面）
    private String cover;
    
    // 出版日期
    private LocalDateTime publishDate;
    
    // 平均评分（如8.5）
    private Double avgRating;
    
    // 评分人数
    private Integer ratingCount;
    
    // 逻辑删除
    @TableLogic
    private Integer isDeleted;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;
}

