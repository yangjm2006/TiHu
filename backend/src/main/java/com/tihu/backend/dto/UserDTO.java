package com.tihu.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户DTO - 用于返回给前端的用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String role;
    private String avatarUrl;      // V1 固定默认头像
    private LocalDateTime banEndTime;  // 封禁截止时间，未封禁为null
    private LocalDateTime createdAt;
}

