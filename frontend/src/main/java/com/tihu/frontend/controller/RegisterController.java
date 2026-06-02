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

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();

    @FXML
    private void onSubmit() {
        try {
            String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText().trim();
            if (username.isBlank() || password.isBlank()) {
                setMessageStyle("error-text");
                messageLabel.setText("用户名和密码不能为空");
                return;
            }
            context.service().registerUser(username, password);
            setMessageStyle("success-text");
            messageLabel.setText("注册成功，请返回登录");
        } catch (Exception ex) {
            setMessageStyle("error-text");
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
            stage.setScene(AppTheme.scene(loader.load(), width, height));
        } catch (Exception ex) {
            messageLabel.setText("跳转失败：" + ex.getMessage());
        }
    }

    private void setMessageStyle(String styleClass) {
        messageLabel.getStyleClass().removeAll("error-text", "success-text");
        messageLabel.getStyleClass().add(styleClass);
    }
}

