package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    
    // 封面，可存 URL 或 data:image/...;base64,...
    @JsonAlias({"coverImage", "coverUrl", "image", "imageUrl"})
    private String cover;
    
    // 出版日期
    private LocalDateTime publishDate;
    
    // 平均评分（如8.5）
    @JsonProperty("averageScore")
    private Double avgRating;
    
    // 评分人数
    private Integer ratingCount;

    // 前端传入的标签名数组，仅用于请求/响应，不落库到 book 表
    @TableField(exist = false)
    private List<String> tags;

    // 逻辑删除
    @TableLogic
    private Integer isDeleted;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;

    @JsonProperty("coverImage")
    public String getCoverImage() {
        return cover;
    }

    @JsonProperty("coverUrl")
    public String getCoverUrl() {
        return cover;
    }

    @JsonProperty("image")
    public String getImage() {
        return cover;
    }

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        return cover;
    }
}

