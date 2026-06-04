package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.UserProfileReturnTarget;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;

import java.time.LocalDateTime;
import java.util.List;

public class AdminUsersController implements MainContentController {

    @FXML private TextField usernameField;
    @FXML private TextField hoursField;
    @FXML private TableView<MockBackendService.AdminUserInfo> usersTableView;
    @FXML private TableColumn<MockBackendService.AdminUserInfo, String> usernameColumn;
    @FXML private TableColumn<MockBackendService.AdminUserInfo, String> roleColumn;
    @FXML private TableColumn<MockBackendService.AdminUserInfo, String> createdAtColumn;
    @FXML private TableColumn<MockBackendService.AdminUserInfo, String> bannedUntilColumn;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private List<MockBackendService.AdminUserInfo> users = List.of();

    @FXML
    public void initialize() {
        usernameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().username()));
        roleColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(roleText(data.getValue().role())));
        createdAtColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(timeText(data.getValue().createdAt())));
        bannedUntilColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(banText(data.getValue().bannedUntil())));
        usersTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected != null) {
                usernameField.setText(selected.username());
            }
        });
        usersTableView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpenProfile();
            }
        });
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
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

    @FXML
    private void onGrantAdmin() {
        try {
            String username = normalizedUsername();
            context.service().grantAdmin(username);
            refresh();
            messageLabel.setText(username + " 已赋予管理员权限");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
        messageLabel.setText("已刷新用户列表");
    }

    @FXML
    private void onOpenProfile() {
        try {
            MockBackendService.AdminUserInfo selected = usersTableView.getSelectionModel().getSelectedItem();
            String username = selected == null ? normalizedUsername() : selected.username();
            if (mainController != null) {
                mainController.openUserProfile(username, UserProfileReturnTarget.ADMIN_USERS);
            }
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void refresh() {
        users = context.service().listAdminUsers();
        usersTableView.getItems().setAll(users);
        String selectedUsername = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (!selectedUsername.isBlank()) {
            users.stream()
                    .filter(user -> selectedUsername.equals(user.username()))
                    .findFirst()
                    .ifPresent(user -> usersTableView.getSelectionModel().select(user));
        }
        if (messageLabel.getText() == null || messageLabel.getText().isBlank()) {
            messageLabel.setText("用户按注册时间从旧到新排序；双击用户可查看主页");
        }
    }

    private String normalizedUsername() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (username.isBlank()) {
            throw new IllegalArgumentException("请输入用户名，或先在用户列表中选择用户");
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

    private String roleText(MockBackendService.Role role) {
        return role == MockBackendService.Role.ADMIN ? "管理员" : "普通用户";
    }

    private String banText(LocalDateTime bannedUntil) {
        return bannedUntil == null ? "正常" : "封禁至 " + bannedUntil;
    }

    private String timeText(LocalDateTime time) {
        return time == null ? "-" : time.toString();
    }
}
