package com.tihu.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 图书详情DTO - 用于返回图书详情页面需要的完整信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailDTO {
    private Map<String, Object> bookInfo;      // 图书基本信息
    private Map<String, Object> ratings;       // 评分统计（平均分、总数、分布、我的评分）
    private Long favoriteCount;               // 收藏人数（去重用户数）
    private Long favoritesCount;               // 收藏人数别名
    private Long collectCount;                // 收藏人数别名
    private Long collectionCount;             // 收藏人数别名
    private Long collectedCount;              // 收藏人数别名
    private List<?> comments;                  // 一级评论列表
    private List<?> replies;                   // 二级回复列表
}

