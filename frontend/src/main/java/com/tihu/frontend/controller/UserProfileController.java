package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class UserProfileController implements MainContentController {

    @FXML private TextField usernameField;
    @FXML private Label userLabel;
    @FXML private Label relationLabel;
    @FXML private ListView<String> commentsListView;
    @FXML private ListView<String> bookListsView;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private String viewedUsername;
    private java.util.List<MockBackendService.UserBookList> profileBookLists = java.util.List.of();

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        usernameField.setText(context.username());
        loadProfile();
    }

    @FXML
    private void onLoadProfile() {
        loadProfile();
    }

    @FXML
    private void onFollow() {
        try {
            context.service().follow(context.username(), usernameField.getText().trim());
            loadProfile();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onUnfollow() {
        try {
            context.service().unfollow(context.username(), usernameField.getText().trim());
            loadProfile();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onOpenBookList() {
        int idx = bookListsView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= profileBookLists.size()) {
            messageLabel.setText("请先选择书单");
            return;
        }
        if (mainController != null) {
            MockBackendService.UserBookList selected = profileBookLists.get(idx);
            mainController.openBookListDetail(viewedUsername, selected.id());
        }
    }

    private void loadProfile() {
        try {
            viewedUsername = usernameField.getText().trim();
            MockBackendService.UserProfile profile = context.service().getUserProfile(viewedUsername, context.username());
            userLabel.setText("用户：" + profile.username());
            relationLabel.setText("关注 " + profile.followingCount() + " | 粉丝 " + profile.followerCount() +
                    " | 我是否已关注：" + (profile.followedByCurrentUser() ? "是" : "否"));
            commentsListView.getItems().setAll(profile.comments().stream()
                    .map(item -> item.time() + " - " + item.content())
                    .toList());
            profileBookLists = profile.bookLists();
            bookListsView.getItems().setAll(profile.bookLists().stream()
                    .map(item -> item.title() + "（" + item.bookIds().size() + "本）")
                    .toList());
            messageLabel.setText("");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }
}

