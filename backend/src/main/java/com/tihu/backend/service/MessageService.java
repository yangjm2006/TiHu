package com.tihu.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tihu.backend.entity.Message;

/**
 * 私信Service
 */
public interface MessageService extends IService<Message> {
    /**
     * 发送私信
     */
    Object sendMessage(Long senderId, Long receiverId, String content) throws Exception;
    
    /**
     * 获取与某用户的对话历史
     */
    Page<Object> getConversation(Long userId, Long otherUserId, int pageNum, int pageSize);
    
    /**
     * 获取对话列表（最后消息聚合）
     */
    Object getConversationList(Long userId);
}

