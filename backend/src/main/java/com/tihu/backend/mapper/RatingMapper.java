package com.tihu.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tihu.backend.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {
    @Select("SELECT score, COUNT(*) as count FROM rating WHERE book_id = #{bookId} GROUP BY score")
    List<java.util.Map<String, Object>> getRatingDistribution(Long bookId);
}

