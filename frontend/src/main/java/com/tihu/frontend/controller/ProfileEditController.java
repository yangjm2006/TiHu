package com.tihu.frontend.controller;

import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ProfileEditController implements MainContentController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();

    @Override
    public void onShow() {
        usernameField.setText(context.username());
        messageLabel.setText("头像为系统默认头像，V1 不支持上传");
    }

    @FXML
    private void onSave() {
        try {
            String oldUsername = context.username();
            context.service().updateProfile(oldUsername, usernameField.getText().trim(), passwordField.getText());
            String newName = usernameField.getText().trim();
            context.login(newName, context.role());
            passwordField.clear();
            messageLabel.setText("修改成功");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }
}

