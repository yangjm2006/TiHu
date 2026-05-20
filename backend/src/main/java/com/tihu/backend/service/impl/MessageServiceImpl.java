package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.entity.Message;
import com.tihu.backend.mapper.MessageMapper;
import com.tihu.backend.service.MessageService;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    public Message sendMessage(Long senderId, Long receiverId, String content) throws Exception {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        this.save(message);
        return message;
    }

    @Override
    public Page<Message> getConversation(Long userId, Long otherUserId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.eq(Message::getSenderId, userId).eq(Message::getReceiverId, otherUserId)
            .or().eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, userId))
            .orderByDesc(Message::getCreateTime);
        
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Object getConversationList(Long userId) {
        // 返回对话列表
        return null;
    }
}

