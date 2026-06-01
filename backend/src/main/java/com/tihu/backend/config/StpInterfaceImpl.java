package com.tihu.backend.config;

import cn.dev33.satoken.stp.StpInterface;
import com.tihu.backend.entity.User;
import com.tihu.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token role adapter.
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        User user = userService.getById(userId);
        if (user == null || user.getRole() == null || user.getRole().isBlank()) {
            return Collections.emptyList();
        }
        return List.of(user.getRole());
    }
}
