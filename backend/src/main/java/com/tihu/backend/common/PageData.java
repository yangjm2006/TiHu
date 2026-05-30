package com.tihu.backend.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.HashMap;
import java.util.Map;

public class PageData<T> {
    private final Object records;
    private final long total;
    private final long pages;
    private final long current;
    private final long size;

    public PageData(Object records, long total, long pages, long current, long size) {
        this.records = records;
        this.total = total;
        this.pages = pages;
        this.current = current;
        this.size = size;
    }

    public Object getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public long getPages() {
        return pages;
    }

    public long getCurrent() {
        return current;
    }

    public long getSize() {
        return size;
    }

    public static <T> PageData<T> of(Page<T> page) {
        return new PageData<>(page.getRecords(), page.getTotal(), page.getPages(), page.getCurrent(), page.getSize());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("records", records);
        map.put("total", total);
        map.put("pages", pages);
        map.put("current", current);
        map.put("size", size);
        return map;
    }
}
