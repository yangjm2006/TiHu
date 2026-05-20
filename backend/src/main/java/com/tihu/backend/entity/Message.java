package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信实体
 */
@Data
@TableName("`message`")
public class Message {
    private Long id;

    // 发送者ID
    private Long senderId;

    // 接收者ID
    private Long receiverId;

    // 消息内容（纯文本）
    private String content;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;
}

