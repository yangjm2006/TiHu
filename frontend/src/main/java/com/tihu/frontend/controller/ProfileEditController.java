package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.ImageDataUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;

public class ProfileEditController implements MainContentController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label currentUserLabel;
    @FXML private Label currentRoleLabel;
    @FXML private ImageView avatarPreview;
    @FXML private Label avatarStatusLabel;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private String selectedAvatarImage;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        usernameField.setText(context.username());
        currentUserLabel.setText("当前登录用户：" + context.username());
        currentRoleLabel.setText("当前角色：" + (context.role() == null ? "未知" : context.role().name()));
        loadAvatar();
        messageLabel.setText("可修改用户名、密码和头像");
    }

    @FXML
    private void onSave() {
        try {
            String oldUsername = context.username();
            context.service().updateProfile(oldUsername, usernameField.getText().trim(), passwordField.getText(),
                    selectedAvatarImage);
            String newName = usernameField.getText().trim();
            context.login(newName, context.role());
            if (mainController != null) {
                mainController.refreshUserInfo();
            }
            passwordField.clear();
            currentUserLabel.setText("当前登录用户：" + context.username());
            currentRoleLabel.setText("当前角色：" + (context.role() == null ? "未知" : context.role().name()));
            messageLabel.setText("修改成功");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onUploadAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择头像");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = chooser.showOpenDialog(avatarPreview == null ? null : avatarPreview.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            selectedAvatarImage = ImageDataUtil.toDataUri(file);
            updateAvatarPreview();
            messageLabel.setText("头像已选择，保存后生效");
        } catch (Exception ex) {
            messageLabel.setText("读取头像失败：" + ex.getMessage());
        }
    }

    @FXML
    private void onClearAvatar() {
        selectedAvatarImage = "";
        updateAvatarPreview();
        messageLabel.setText("头像已清空，保存后生效");
    }

    private void loadAvatar() {
        try {
            MockBackendService.UserProfile profile = context.service().getUserProfile(context.username(), context.username());
            selectedAvatarImage = profile.avatarImage();
        } catch (Exception ex) {
            selectedAvatarImage = null;
        }
        updateAvatarPreview();
    }

    private void updateAvatarPreview() {
        if (avatarPreview == null) {
            return;
        }
        Image image = ImageDataUtil.image(selectedAvatarImage);
        avatarPreview.setImage(image);
        if (avatarStatusLabel != null) {
            avatarStatusLabel.setText(image == null ? "当前无头像" : "已选择头像");
        }
    }
}

