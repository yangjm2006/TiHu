package com.tihu.backend.common;

/**
 * 系统常量
 */
public final class Constants {
    
    // 角色常量
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    
    // 邀请码
    public static final String ADMIN_INVITE_CODE = "123456";
    
    // 用户名长度限制
    public static final int USERNAME_MIN_LENGTH = 2;
    public static final int USERNAME_MAX_LENGTH = 10;
    
    // 密码长度限制
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 12;
    
    // 评论长度限制
    public static final int COMMENT_MAX_LENGTH = 200;
    
    // 分页
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    // Redis Key 前缀
    public static final String REDIS_KEY_USER_RATING = "user:rating:";
    public static final String REDIS_KEY_BOOK_RATING_STATS = "book:rating:stats:";
    public static final String REDIS_KEY_USER_LIKES = "user:likes:";
    public static final String REDIS_KEY_USER_DISLIKES = "user:dislikes:";
    public static final String REDIS_KEY_USER_COLLECTIONS = "user:collections:";
    public static final String REDIS_KEY_USER_FOLLOWERS = "user:followers:";
    public static final String REDIS_KEY_USER_FOLLOWEES = "user:followees:";
    
    // Session 过期时间（24小时，单位秒）
    public static final int SESSION_TIMEOUT = 86400;
}

