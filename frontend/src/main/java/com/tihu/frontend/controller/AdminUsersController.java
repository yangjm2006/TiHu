package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;

public class AdminUsersController implements MainContentController {

    @FXML private TextField usernameField;
    @FXML private TextField hoursField;
    @FXML private ListView<String> bannedListView;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void onBan() {
        try {
            long hours = Long.parseLong(hoursField.getText().trim());
            context.service().banUser(usernameField.getText().trim(), LocalDateTime.now().plusHours(hours));
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onUnban() {
        try {
            context.service().unbanUser(usernameField.getText().trim());
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void refresh() {
        bannedListView.getItems().setAll(context.service().listBans().stream()
                .map(info -> info.username() + " 禁封至 " + info.bannedUntil())
                .toList());
        messageLabel.setText("封禁用户后将无法登录");
    }
}

