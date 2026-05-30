package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService.FollowItem;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class FollowingController implements MainContentController {

    @FXML private Label titleLabel;
    @FXML private ListView<String> listView;
    @FXML private Label messageLabel;
    @FXML private Button followToggleButton;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private List<FollowItem> followItems = List.of();
    private boolean isFollowersMode;

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
        isFollowersMode = context.isShowingFollowers();
        String currentUser = context.username();

        if (isFollowersMode) {
            titleLabel.setText(target.equals(currentUser) ? "我的粉丝" : "「" + target + "」的粉丝");
            followItems = context.service().listFollowers(target, currentUser);
        } else {
            titleLabel.setText(target.equals(currentUser) ? "我的关注" : "「" + target + "」的关注");
            followItems = context.service().listFollowing(target, currentUser);
        }

        refreshListView();
        messageLabel.setText(followItems.isEmpty() ? "暂无数据" : "双击用户可查看其主页");

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenProfile();
            }
        });

        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            updateFollowButtonState();
        });
        updateFollowButtonState();
    }

    private void refreshListView() {
        List<String> displayItems = followItems.stream()
                .map(item -> {
                    String marker = item.followedByCurrentUser() ? " [已关注]" : "";
                    return item.username() + marker;
                })
                .toList();
        listView.getItems().setAll(displayItems);
    }

    private void updateFollowButtonState() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            followToggleButton.setText("关注/取关");
            followToggleButton.setDisable(true);
            return;
        }
        FollowItem selected = followItems.get(idx);
        if (selected.username().equals(context.username())) {
            followToggleButton.setText("这是你自己");
            followToggleButton.setDisable(true);
        } else if (selected.followedByCurrentUser()) {
            followToggleButton.setText("取消关注");
            followToggleButton.setDisable(false);
        } else {
            followToggleButton.setText("关注");
            followToggleButton.setDisable(false);
        }
    }

    @FXML
    private void onOpenProfile() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        if (mainController != null) {
            mainController.openUserProfile(followItems.get(idx).username());
        }
    }

    @FXML
    private void onToggleFollow() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        FollowItem selected = followItems.get(idx);
        String target = selected.username();
        String me = context.username();

        if (target.equals(me)) {
            messageLabel.setText("不能关注自己");
            return;
        }

        try {
            if (selected.followedByCurrentUser()) {
                context.service().unfollow(me, target);
                messageLabel.setText("已取消关注 " + target);
            } else {
                context.service().follow(me, target);
                messageLabel.setText("已关注 " + target);
            }
            onShow();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }
}

    private void refreshListView() {
        List<String> displayItems = followItems.stream()
                .map(item -> {
                    String marker = item.followedByCurrentUser() ? " [已关注]" : "";
                    return item.username() + marker;
                })
                .toList();
        listView.getItems().setAll(displayItems);
    }

    private void updateFollowButtonState() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            followToggleButton.setText("关注/取关");
            followToggleButton.setDisable(true);
            return;
        }
        FollowItem selected = followItems.get(idx);
        if (selected.username().equals(context.username())) {
            followToggleButton.setText("这是你自己");
            followToggleButton.setDisable(true);
        } else if (selected.followedByCurrentUser()) {
            followToggleButton.setText("取消关注");
            followToggleButton.setDisable(false);
        } else {
            followToggleButton.setText("关注");
            followToggleButton.setDisable(false);
        }
    }

    @FXML
    private void onOpenProfile() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        if (mainController != null) {
            mainController.openUserProfile(followItems.get(idx).username());
        }
    }

    @FXML
    private void onToggleFollow() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        FollowItem selected = followItems.get(idx);
        String target = selected.username();
        String me = context.username();

        if (target.equals(me)) {
            messageLabel.setText("不能关注自己");
            return;
        }

        try {
            if (selected.followedByCurrentUser()) {
                context.service().unfollow(me, target);
                messageLabel.setText("已取消关注 " + target);
            } else {
                context.service().follow(me, target);
                messageLabel.setText("已关注 " + target);
            }
            onShow();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }
}



    private void refreshListView() {
        List<String> displayItems = followItems.stream()
                .map(item -> {
                    String marker = item.followedByCurrentUser() ? " [已关注]" : "";
                    return item.username() + marker;
                })
                .toList();
        listView.getItems().setAll(displayItems);
    }

    private void updateFollowButtonState() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            followToggleButton.setText("关注/取关");
            followToggleButton.setDisable(true);
            return;
        }
        FollowItem selected = followItems.get(idx);
        if (selected.username().equals(context.username())) {
            followToggleButton.setText("这是你自己");
            followToggleButton.setDisable(true);
        } else if (selected.followedByCurrentUser()) {
            followToggleButton.setText("取消关注");
            followToggleButton.setDisable(false);
        } else {
            followToggleButton.setText("关注");
            followToggleButton.setDisable(false);
        }
    }

    @FXML
    private void onOpenProfile() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        if (mainController != null) {
            mainController.openUserProfile(followItems.get(idx).username());
        }
    }

    @FXML
    private void onToggleFollow() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        FollowItem selected = followItems.get(idx);
        String target = selected.username();
        String me = context.username();

        if (target.equals(me)) {
            messageLabel.setText("不能关注自己");
            return;
        }

        try {
            if (selected.followedByCurrentUser()) {
                context.service().unfollow(me, target);
                messageLabel.setText("已取消关注 " + target);
            } else {
                context.service().follow(me, target);
                messageLabel.setText("已关注 " + target);
            }
            // 重新加载列表
            onShow();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }
}

    private void refreshListView() {
        List<String> displayItems = followItems.stream()
                .map(item -> {
                    String marker = item.followedByCurrentUser() ? " [已关注]" : "";
                    return item.username() + marker;
                })
                .toList();
        listView.getItems().setAll(displayItems);
    }

    private void updateFollowButtonState() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            followToggleButton.setText("关注/取关");
            followToggleButton.setDisable(true);
            return;
        }
        FollowItem selected = followItems.get(idx);
        if (selected.username().equals(context.username())) {
            followToggleButton.setText("这是你自己");
            followToggleButton.setDisable(true);
        } else if (selected.followedByCurrentUser()) {
            followToggleButton.setText("取消关注");
            followToggleButton.setDisable(false);
        } else {
            followToggleButton.setText("关注");
            followToggleButton.setDisable(false);
        }
    }

    @FXML
    private void onOpenProfile() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        if (mainController != null) {
            mainController.openUserProfile(followItems.get(idx).username());
        }
    }

    @FXML
    private void onToggleFollow() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= followItems.size()) {
            messageLabel.setText("请先选择用户");
            return;
        }
        FollowItem selected = followItems.get(idx);
        String target = selected.username();
        String me = context.username();

        if (target.equals(me)) {
            messageLabel.setText("不能关注自己");
            return;
        }

        try {
            if (selected.followedByCurrentUser()) {
                context.service().unfollow(me, target);
                messageLabel.setText("已取消关注 " + target);
            } else {
                context.service().follow(me, target);
                messageLabel.setText("已关注 " + target);
            }
            // 重新加载列表
            onShow();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }
}

