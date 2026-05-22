package com.tihu.frontend.controller;

import com.tihu.frontend.MainApplication;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AdminRegisterController {

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
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("用户名、密码和邀请码不能为空");
                return;
            }
            context.service().registerAdmin(username, password, inviteCode);
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("管理员注册成功，请返回登录");
        } catch (Exception ex) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(ex.getMessage());
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
            stage.setScene(new Scene(loader.load(), width, height));
        } catch (Exception ex) {
            messageLabel.setText("跳转失败：" + ex.getMessage());
        }
    }
}

