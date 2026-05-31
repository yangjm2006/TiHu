package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.Message;
import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.MessageMapper;
import com.tihu.backend.mapper.UserMapper;
import com.tihu.backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public Object sendMessage(Long senderId, Long receiverId, String content) throws Exception {
        if (senderId.equals(receiverId)) {
            throw new ApiException(400, "不能给自己发送私信");
        }
        if (!StringUtils.hasText(content)) {
            throw new ApiException(400, "消息内容不能为空");
        }
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            throw new ApiException(404, "用户不存在");
        }
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content.trim());
        this.save(message);
        return toMessageRecord(message);
    }

    @Override
    public Page<Object> getConversation(Long userId, Long otherUserId, int pageNum, int pageSize) {
        if (userMapper.selectById(otherUserId) == null) {
            throw new ApiException(404, "用户不存在");
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.eq(Message::getSenderId, userId).eq(Message::getReceiverId, otherUserId)
            .or().eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, userId))
            .orderByAsc(Message::getCreateTime);
        
        Page<Message> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return toMessagePage(page);
    }

    @Override
    public Object getConversationList(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSenderId, userId)
            .or()
            .eq(Message::getReceiverId, userId)
            .orderByDesc(Message::getCreateTime);

        Map<Long, Object> conversations = new LinkedHashMap<>();
        for (Message message : this.list(wrapper)) {
            Long peerId = message.getSenderId().equals(userId) ? message.getReceiverId() : message.getSenderId();
            conversations.putIfAbsent(peerId, toConversationRecord(peerId, message));
        }

        List<Object> records = new ArrayList<>(conversations.values());
        Page<Object> page = new Page<>(1, Math.max(records.size(), 1), records.size());
        page.setRecords(records);
        page.setPages(1);
        return com.tihu.backend.common.PageData.of(page);
    }

    private Page<Object> toMessagePage(Page<Message> messagePage) {
        Page<Object> result = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
        result.setPages(messagePage.getPages());
        result.setRecords(messagePage.getRecords().stream()
            .map(this::toMessageRecord)
            .<Object>map(record -> record)
            .toList());
        return result;
    }

    private Map<String, Object> toMessageRecord(Message message) {
        User sender = userMapper.selectById(message.getSenderId());
        User receiver = userMapper.selectById(message.getReceiverId());

        Map<String, Object> record = new HashMap<>();
        record.put("id", message.getId());
        record.put("from", sender != null ? sender.getUsername() : null);
        record.put("to", receiver != null ? receiver.getUsername() : null);
        record.put("content", message.getContent());
        record.put("message", message.getContent());
        record.put("time", message.getCreateTime());
        record.put("createdAt", message.getCreateTime());
        record.put("senderId", message.getSenderId());
        record.put("receiverId", message.getReceiverId());
        record.put("senderUser", sender);
        record.put("receiverUser", receiver);
        return record;
    }

    private Map<String, Object> toConversationRecord(Long peerId, Message lastMessage) {
        User peer = userMapper.selectById(peerId);
        Map<String, Object> record = new HashMap<>();
        record.put("peer", peer != null ? peer.getUsername() : null);
        record.put("peerUsername", peer != null ? peer.getUsername() : null);
        record.put("peerUser", peer);
        record.put("lastMessage", lastMessage.getContent());
        record.put("lastContent", lastMessage.getContent());
        record.put("lastTime", lastMessage.getCreateTime());
        record.put("updatedAt", lastMessage.getCreateTime());
        return record;
    }
}

