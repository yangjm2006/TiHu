package com.tihu.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tihu.backend.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}

