package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 图书-标签关联表（多对多）
 */
@Data
@TableName("`book_tag`")
public class BookTag {
    private Long id;
    
    // 图书ID
    private Long bookId;
    
    // 标签ID
    private Long tagId;
}

