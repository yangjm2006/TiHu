-- 鹈鹕系统数据库初始化脚本

-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(10) NOT NULL UNIQUE COMMENT '用户名（唯一）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    `status` INT DEFAULT 0 COMMENT '0=正常，1=封禁',
    `ban_expire_time` DATETIME COMMENT '封禁过期时间',
    `bio` VARCHAR(500) COMMENT '用户简介',
    `avatar` LONGTEXT COMMENT '头像URL或data URI',
    `name` VARCHAR(50) COMMENT '旧字段',
    `age` INT COMMENT '旧字段',
    `email` VARCHAR(100) COMMENT '旧字段',
    `is_deleted` INT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

ALTER TABLE `user` MODIFY COLUMN `avatar` LONGTEXT COMMENT '头像URL或data URI';

-- 创建角色表
CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(500),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建用户角色关联表
CREATE TABLE IF NOT EXISTS `user_role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `role_name` VARCHAR(50) NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_role` (`user_id`, `role_name`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建图书表
CREATE TABLE IF NOT EXISTS `book` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL UNIQUE,
    `author` VARCHAR(100),
    `isbn` VARCHAR(50),
    `description` TEXT,
    `cover` LONGTEXT,
    `publish_date` DATETIME,
    `avg_rating` DECIMAL(3, 1) DEFAULT 0,
    `rating_count` INT DEFAULT 0,
    `is_deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_title` (`title`),
    KEY `idx_avg_rating` (`avg_rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `book` MODIFY COLUMN `cover` LONGTEXT;

-- 创建标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(500),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建图书标签关联表
CREATE TABLE IF NOT EXISTS `book_tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `book_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    UNIQUE KEY `uk_book_tag` (`book_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建评分表
CREATE TABLE IF NOT EXISTS `rating` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `score` INT NOT NULL COMMENT '1-10',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_book_rating` (`user_id`, `book_id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `content` VARCHAR(200) NOT NULL,
    `parent_comment_id` BIGINT COMMENT '父评论ID',
    `like_count` INT DEFAULT 0,
    `dislike_count` INT DEFAULT 0,
    `is_deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_book_id` (`book_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建评论点赞踩表
CREATE TABLE IF NOT EXISTS `comment_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `comment_id` BIGINT NOT NULL,
    `like_type` INT NOT NULL COMMENT '0=赞, 1=踩',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_comment_like` (`user_id`, `comment_id`),
    KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建收藏表
CREATE TABLE IF NOT EXISTS `collection` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建书单表
CREATE TABLE IF NOT EXISTS `book_list` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `cover` VARCHAR(500),
    `is_public` BOOLEAN DEFAULT TRUE,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建书单项目表
CREATE TABLE IF NOT EXISTS `book_list_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `book_list_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `sort_order` INT DEFAULT 0,
    UNIQUE KEY `uk_list_book` (`book_list_id`, `book_id`),
    KEY `idx_book_list_id` (`book_list_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建关注表
CREATE TABLE IF NOT EXISTS `follow` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `follower_id` BIGINT NOT NULL COMMENT '粉丝ID',
    `followee_id` BIGINT NOT NULL COMMENT '被关注者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_follow` (`follower_id`, `followee_id`),
    KEY `idx_follower_id` (`follower_id`),
    KEY `idx_followee_id` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建私信表
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `sender_id` BIGINT NOT NULL,
    `receiver_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_conversation` (`sender_id`, `receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化角色数据
INSERT IGNORE INTO `role` (`role_name`, `description`) VALUES ('USER', '普通用户');
INSERT IGNORE INTO `role` (`role_name`, `description`) VALUES ('ADMIN', '管理员');

