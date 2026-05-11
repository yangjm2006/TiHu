package com.tihu.frontend.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        roleBox.getItems().addAll("普通用户", "管理员");
        roleBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        String role = roleBox.getValue();

        if (user.isBlank() || pass.isBlank()) {
            messageLabel.setText("用户名或密码不能为空");
            return;
        }
        messageLabel.setText("登录成功（演示）：" + user + " / " + role);
    }

    @FXML
    private void onRegister() {
        messageLabel.setText("注册功能后续接入");
    }
}