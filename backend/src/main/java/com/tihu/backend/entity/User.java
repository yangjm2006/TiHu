package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("`user`")
public class User {
    private Long id;
    
    // 用户名（唯一，大小写敏感）
    private String username;
    
    // 密码（加密存储）
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    // 用户角色
    private String role;
    
    // 用户状态：0=正常，1=封禁
    private Integer status;
    
    // 封禁过期时间
    private LocalDateTime banExpireTime;
    
    // 用户简介
    private String bio;
    
    // 头像URL（V1使用默认头像）
    private String avatar;
    
    // 旧字段（保留向后兼容）
    private String name;
    private Integer age;
    private String email;
    
    // 逻辑删除标识
    @TableLogic
    private Integer isDeleted;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;
}
