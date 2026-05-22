package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.PageData;
import com.tihu.backend.common.Result;
import com.tihu.backend.entity.Message;
import com.tihu.backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 私信相关接口
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送私信
     * POST /api/messages?receiverId=xxx&content=xxx
     */
    @PostMapping
    public Result sendMessage(@RequestParam Long receiverId, @RequestParam String content) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Message message = messageService.sendMessage(userId, receiverId, content);
        return Result.success(message);
    }

    /**
     * 获取与某用户的对话历史
     * GET /api/messages/conversation/{userId}?page=1&size=10
     */
    @GetMapping("/conversation/{userId}")
    public Result getConversation(@PathVariable Long userId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<Message> messages = messageService.getConversation(currentUserId, userId, page, size);
        return Result.success(PageData.of(messages));
    }

    /**
     * 获取对话列表
     * GET /api/messages/conversations
     */
    @GetMapping("/conversations")
    public Result getConversationList() {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Object list = messageService.getConversationList(userId);
        return Result.success(list);
    }
}

