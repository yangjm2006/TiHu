package com.tihu.frontend.controller1;

import com.tihu.frontend.MainApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

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
    private void onLogin() throws Exception{
        String user = usernameField.getText();
        String pass = passwordField.getText();
        String role = roleBox.getValue();

        if (user.isBlank() || pass.isBlank()) {
            messageLabel.setText("用户名或密码不能为空");
            return;
        }
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(scene);
    }

    @FXML
    private void onRegister() {
        messageLabel.setText("注册功能后续接入");
    }
}