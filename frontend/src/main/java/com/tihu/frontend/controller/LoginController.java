package com.tihu.frontend.controller;

import com.tihu.frontend.MainApplication;
import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppTheme;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.concurrent.Task;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button themeToggleButton;

    private final AppContext context = AppContext.getInstance();

    @FXML
    public void initialize() {
        updateThemeToggleText();
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (user == null || user.isBlank() || pass == null || pass.isBlank()) {
            messageLabel.setText("用户名或密码不能为空");
            return;
        }
        Task<MockBackendService.Role> loginTask = new Task<>() {
            @Override
            protected MockBackendService.Role call() throws Exception {
                return context.service().login(user.trim(), pass.trim());
            }
        };

        loginTask.setOnSucceeded(event -> {
            MockBackendService.Role role = loginTask.getValue();
            context.login(user.trim(), role);
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
                Scene scene = AppTheme.scene(fxmlLoader.load(), 1380, 860);
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(scene);
            } catch (Exception ex) {
                messageLabel.setText(ex.getMessage());
            }
        });

        loginTask.setOnFailed(event -> {
            Throwable ex = loginTask.getException();
            Throwable root = ex == null ? null : (ex.getCause() == null ? ex : ex.getCause());
            messageLabel.setText(root == null || root.getMessage() == null || root.getMessage().isBlank()
                    ? "登录失败"
                    : root.getMessage());
        });

        Thread thread = new Thread(loginTask, "tihu-login-thread");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onOpenUserRegister() {
        switchScene("register-view.fxml", 580, 420);
    }

    @FXML
    private void onOpenAdminRegister() {
        switchScene("admin-register-view.fxml", 580, 460);
    }

    private void switchScene(String fxml, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(fxml));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(AppTheme.scene(loader.load(), width, height));
        } catch (Exception ex) {
            messageLabel.setText("页面跳转失败：" + ex.getMessage());
        }
    }

    @FXML
    private void onToggleTheme() {
        AppTheme.toggle(usernameField.getScene());
        updateThemeToggleText();
    }

    private void updateThemeToggleText() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(AppTheme.toggleButtonText());
        }
    }
}
