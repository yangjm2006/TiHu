package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.entity.Tag;
import com.tihu.backend.mapper.TagMapper;
import com.tihu.backend.service.TagService;
import org.springframework.stereotype.Service;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
}

