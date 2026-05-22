package com.tihu.backend.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 分页数据统一包装类
 * 用于兼容前端期望的分页格式
 */
@Data
public class PageData<T> {
    private List<T> records;      // 记录列表
    private long total;           // 总记录数
    private long pages;           // 总页数
    private long current;         // 当前页码
    private long size;            // 每页大小

    public static <T> PageData<T> of(Page<T> page) {
        PageData<T> pageData = new PageData<>();
        pageData.setRecords(page.getRecords());
        pageData.setTotal(page.getTotal());
        pageData.setPages(page.getPages());
        pageData.setCurrent(page.getCurrent());
        pageData.setSize(page.getSize());
        return pageData;
    }
}

