package com.tihu.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 书单中的书籍项目
 */
@Data
@TableName("`book_list_item`")
public class BookListItem {
    private Long id;
    
    // 书单ID
    private Long bookListId;
    
    // 图书ID
    private Long bookId;
    
    // 书在书单中的排序位置
    private Integer sortOrder;
}

