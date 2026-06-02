package com.tihu.frontend.controller;

import com.tihu.frontend.MainApplication;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppTheme;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AdminRegisterController {
    private static final String ADMIN_INVITE_CODE = "123456";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField inviteCodeField;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();

    @FXML
    private void onSubmit() {
        try {
            String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText().trim();
            String inviteCode = inviteCodeField.getText() == null ? "" : inviteCodeField.getText().trim();
            if (username.isBlank() || password.isBlank() || inviteCode.isBlank()) {
                setMessageStyle("error-text");
                messageLabel.setText("用户名、密码和邀请码不能为空");
                return;
            }
            if (!ADMIN_INVITE_CODE.equals(inviteCode)) {
                setMessageStyle("error-text");
                messageLabel.setText("邀请码错误，注册失败");
                return;
            }
            context.service().registerAdmin(username, password, inviteCode);
            setMessageStyle("success-text");
            messageLabel.setText("管理员注册成功，请返回登录");
        } catch (Exception ex) {
            setMessageStyle("error-text");
            messageLabel.setText(normalizeRegisterError(ex));
        }
    }

    @FXML
    private void onBack() {
        switchScene("login-view.fxml", 520, 380);
    }

    private void switchScene(String fxml, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(fxml));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(AppTheme.scene(loader.load(), width, height));
        } catch (Exception ex) {
            messageLabel.setText("跳转失败：" + ex.getMessage());
        }
    }

    private void setMessageStyle(String styleClass) {
        messageLabel.getStyleClass().removeAll("error-text", "success-text");
        messageLabel.getStyleClass().add(styleClass);
    }

    private String normalizeRegisterError(Exception ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("邀请码")) {
            return "邀请码错误，注册失败";
        }
        return message == null || message.isBlank() ? "注册失败" : message;
    }
}

