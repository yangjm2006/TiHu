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

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();

    @FXML
    private void onSubmit() {
        try {
            context.service().registerUser(usernameField.getText(), passwordField.getText());
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("注册成功，请返回登录");
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

