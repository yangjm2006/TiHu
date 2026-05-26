package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.Tag;
import com.tihu.backend.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签接口
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    /**
     * 获取标签列表
     * GET /api/tags
     */
    @GetMapping
    public Result getTags() {
        List<Tag> tags = tagService.list(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName));
        return Result.success(tags);
    }

    /**
     * 创建标签（管理员）
     * POST /api/tags
     */
    @PostMapping
    public Result createTag(@RequestBody Tag tag) {
        StpUtil.checkRole("ADMIN");
        normalizeTag(tag);
        if (tagService.lambdaQuery().eq(Tag::getName, tag.getName()).count() > 0) {
            throw new ApiException(409, "标签已存在");
        }
        tagService.save(tag);
        return Result.success(tag);
    }

    private void normalizeTag(Tag tag) {
        if (tag == null || tag.getName() == null || tag.getName().trim().isEmpty()) {
            throw new ApiException(400, "标签名不能为空");
        }
        tag.setName(tag.getName().trim());
        if (tag.getDescription() != null) {
            tag.setDescription(tag.getDescription().trim());
        }
    }
}

