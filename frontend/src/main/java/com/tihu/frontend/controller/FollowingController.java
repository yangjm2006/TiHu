package com.tihu.frontend.controller;

import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Locale;

public class FollowingController implements MainContentController {

    @FXML private Label titleLabel;
    @FXML private Label countLabel;
    @FXML private TextField keywordField;
    @FXML private ListView<String> listView;
    @FXML private Label messageLabel;
    @FXML private Button followToggleButton;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private String targetUsername;
    private boolean showingFollowers;
    private List<String> allUsernames = List.of();
    private List<String> filteredUsernames = List.of();

    @FXML
    private void initialize() {
        listView.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> refreshSelectionState());
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenProfile();
            }
        });
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        targetUsername = context.followListTargetUsername();
        if (targetUsername == null || targetUsername.isBlank()) {
            targetUsername = context.username();
        }
        showingFollowers = context.isShowingFollowers();
        loadList();
    }

    @FXML
    private void onShowFollowing() {
        showingFollowers = false;
        context.setShowingFollowers(false);
        loadList();
    }

    @FXML
    private void onShowFollowers() {
        showingFollowers = true;
        context.setShowingFollowers(true);
        loadList();
    }

    @FXML
    private void onRefresh() {
        loadList();
    }

    @FXML
    private void onFilter() {
        applyFilter();
    }

    @FXML
    private void onClearFilter() {
        keywordField.clear();
        applyFilter();
    }

    @FXML
    private void onToggleFollow() {
        String username = selectedUsername();
        if (username == null) {
            messageLabel.setText("请先选择用户");
            return;
        }
        if (username.equals(context.username())) {
            messageLabel.setText("不能关注自己");
            return;
        }
        try {
            boolean followed = context.service().getUserProfile(username, context.username()).followedByCurrentUser();
            String successMessage;
            if (followed) {
                context.service().unfollow(context.username(), username);
                successMessage = "已取消关注 " + username;
            } else {
                context.service().follow(context.username(), username);
                successMessage = "已关注 " + username;
            }
            loadList();
            messageLabel.setText(successMessage);
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onOpenProfile() {
        String username = selectedUsername();
        if (username == null) {
            messageLabel.setText("请先选择用户");
            return;
        }
        if (mainController != null) {
            mainController.openUserProfile(username);
        }
    }

    private void loadList() {
        try {
            titleLabel.setText(buildTitle());
            allUsernames = showingFollowers
                    ? context.service().listFollowers(targetUsername).stream().map(item -> item.username()).toList()
                    : context.service().listFollowing(targetUsername).stream().map(item -> item.username()).toList();
            applyFilter();
            messageLabel.setText(allUsernames.isEmpty() ? "暂无数据" : "双击用户可查看其主页");
        } catch (Exception ex) {
            allUsernames = List.of();
            filteredUsernames = List.of();
            listView.getItems().clear();
            countLabel.setText("共 0 人");
            messageLabel.setText(ex.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim().toLowerCase(Locale.ROOT);
        filteredUsernames = allUsernames.stream()
                .filter(username -> keyword.isBlank() || username.toLowerCase(Locale.ROOT).contains(keyword))
                .toList();
        listView.getItems().setAll(filteredUsernames);
        countLabel.setText("共 " + allUsernames.size() + " 人，当前显示 " + filteredUsernames.size() + " 人");
        refreshSelectionState();
    }

    private void refreshSelectionState() {
        String username = selectedUsername();
        followToggleButton.setDisable(username == null || username.equals(context.username()));
        if (username == null) {
            followToggleButton.setText("关注/取消关注");
            return;
        }
        if (username.equals(context.username())) {
            followToggleButton.setText("当前用户");
            return;
        }
        try {
            boolean followed = context.service().getUserProfile(username, context.username()).followedByCurrentUser();
            followToggleButton.setText(followed ? "取消关注" : "关注");
        } catch (Exception ex) {
            followToggleButton.setText("关注/取消关注");
        }
    }

    private String selectedUsername() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= filteredUsernames.size()) {
            return null;
        }
        return filteredUsernames.get(idx);
    }

    private String buildTitle() {
        boolean mine = targetUsername.equals(context.username());
        if (showingFollowers) {
            return mine ? "我的粉丝" : "“" + targetUsername + "” 的粉丝";
        }
        return mine ? "我的关注" : "“" + targetUsername + "” 的关注";
    }
}

