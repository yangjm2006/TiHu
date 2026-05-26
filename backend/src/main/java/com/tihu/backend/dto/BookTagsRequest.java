package com.tihu.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * 图书标签更新请求
 */
@Data
public class BookTagsRequest {
    private List<String> tags;
}

