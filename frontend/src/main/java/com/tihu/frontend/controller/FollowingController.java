package com.tihu.frontend.controller;

import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class FollowingController implements MainContentController {

    @FXML private Label titleLabel;
    @FXML private ListView<String> listView;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private List<String> usernames = List.of();

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        String target = context.followListTargetUsername();
        if (target == null || target.isBlank()) {
            target = context.username();
        }
        if (context.isShowingFollowers()) {
            titleLabel.setText(target.equals(context.username()) ? "我的粉丝" : "“" + target + "” 的粉丝");
            usernames = context.service().listFollowers(target).stream().map(item -> item.username()).toList();
        } else {
            titleLabel.setText(target.equals(context.username()) ? "我的关注" : "“" + target + "” 的关注");
            usernames = context.service().listFollowing(target).stream().map(item -> item.username()).toList();
        }
        listView.getItems().setAll(usernames);
        messageLabel.setText(usernames.isEmpty() ? "暂无数据" : "双击用户可查看其主页");
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenProfile();
            }
        });
    }

    @FXML
    private void onOpenProfile() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= usernames.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        if (mainController != null) {
            mainController.openUserProfile(usernames.get(idx));
        }
    }
}

