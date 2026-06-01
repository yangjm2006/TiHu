package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.util.List;

public class AdminUsersController implements MainContentController {

    @FXML private TextField usernameField;
    @FXML private TextField hoursField;
    @FXML private ListView<String> bannedListView;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private List<MockBackendService.BanInfo> bans = List.of();

    @FXML
    public void initialize() {
        bannedListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            int idx = newValue == null ? -1 : newValue.intValue();
            if (idx >= 0 && idx < bans.size()) {
                usernameField.setText(bans.get(idx).username());
            }
        });
    }

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void onBan() {
        try {
            String username = normalizedUsername();
            long hours = parseBanHours();
            LocalDateTime until = LocalDateTime.now().plusHours(hours);
            context.service().banUser(username, until);
            refresh();
            messageLabel.setText(username + " 已封禁至 " + until);
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onUnban() {
        try {
            String username = normalizedUsername();
            context.service().unbanUser(username);
            refresh();
            messageLabel.setText(username + " 已解除封禁");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void refresh() {
        bans = context.service().listBans();
        bannedListView.getItems().setAll(bans.stream()
                .map(info -> info.username() + " 封禁至 " + info.bannedUntil())
                .toList());
        if (messageLabel.getText() == null || messageLabel.getText().isBlank()) {
            messageLabel.setText("封禁用户后将无法登录；可选中列表用户后解封");
        }
    }

    private String normalizedUsername() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (username.isBlank()) {
            throw new IllegalArgumentException("请输入用户名，或先在封禁列表中选择用户");
        }
        return username;
    }

    private long parseBanHours() {
        String value = hoursField.getText() == null ? "" : hoursField.getText().trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("请输入封禁小时数");
        }
        long hours = Long.parseLong(value);
        if (hours <= 0) {
            throw new IllegalArgumentException("封禁时长必须大于 0 小时");
        }
        return hours;
    }
}

