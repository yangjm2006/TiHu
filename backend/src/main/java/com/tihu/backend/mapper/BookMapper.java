package com.tihu.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tihu.backend.entity.Book;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
}

