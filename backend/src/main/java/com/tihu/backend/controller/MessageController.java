package com.tihu.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.common.PageData;
import com.tihu.backend.common.Result;
import com.tihu.backend.service.MessageService;
import com.tihu.backend.service.UserService;
import com.tihu.backend.entity.User;
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

    @Autowired
    private UserService userService;

    /**
     * 发送私信
     * POST /api/messages?receiverId=xxx&content=xxx 或 /api/messages?receiverUsername=xxx&content=xxx
     */
    @PostMapping
    public Result sendMessage(@RequestParam(required = false) Long receiverId,
                              @RequestParam(required = false) String receiverUsername,
                              @RequestParam String content) throws Exception {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Object message = messageService.sendMessage(userId, resolvePeerId(receiverId, receiverUsername), content);
        return Result.success(message);
    }

    /**
     * 获取与某用户的对话历史
     * GET /api/messages/conversation/{peerIdOrUsername}?page=1&size=100
     */
    @GetMapping("/conversation/{peer}")
    public Result getConversation(@PathVariable String peer,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "100") int size) {
        Long currentUserId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<Object> messages = messageService.getConversation(currentUserId, resolvePeerId(peer), page, size);
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

    private Long resolvePeerId(Long peerId, String username) {
        if (peerId != null) {
            return peerId;
        }
        if (username == null || username.isBlank()) {
            throw new ApiException(400, "receiverId或receiverUsername不能为空");
        }
        return resolvePeerId(username.trim());
    }

    private Long resolvePeerId(String identifier) {
        if (identifier.matches("\\d+")) {
            return Long.parseLong(identifier);
        }
        User user = userService.getUserByUsername(identifier);
        if (user == null) {
            throw new ApiException(404, "用户不存在");
        }
        return user.getId();
    }
}

