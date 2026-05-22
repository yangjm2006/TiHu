package com.tihu.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户主页DTO - 用于返回用户主页的完整信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private UserDTO userInfo;           // 用户基础信息
    private List<?> comments;           // TA的评论列表（时间倒序）
    private List<?> bookLists;          // TA的公开书单列表
    private Long followingCount;        // 关注数
    private Long followerCount;         // 粉丝数
    private Boolean followedByCurrentUser;  // 当前登录用户是否已关注TA
}

