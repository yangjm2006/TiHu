package com.tihu.frontend.controller;

import com.tihu.frontend.MainApplication;
import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();

    @FXML
    private void onLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (user == null || user.isBlank() || pass == null || pass.isBlank()) {
            messageLabel.setText("用户名或密码不能为空");
            return;
        }
        try {
            MockBackendService.Role role = context.service().login(user.trim(), pass.trim());
            context.login(user.trim(), role);

            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1220, 760);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onOpenUserRegister() {
        switchScene("register-view.fxml", 500, 360);
    }

    @FXML
    private void onOpenAdminRegister() {
        switchScene("admin-register-view.fxml", 500, 400);
    }

    private void switchScene(String fxml, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(fxml));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), width, height));
        } catch (Exception ex) {
            messageLabel.setText("页面跳转失败：" + ex.getMessage());
        }
    }
}