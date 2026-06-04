package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.BookListDetailReturnTarget;
import com.tihu.frontend.utils.AppContext.UserProfileReturnTarget;
import com.tihu.frontend.utils.ImageDataUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;

public class UserProfileController implements MainContentController {

    @FXML private TextField usernameField;
    @FXML private Label userLabel;
    @FXML private Label relationLabel;
    @FXML private ImageView avatarImageView;
    @FXML private ListView<String> commentsListView;
    @FXML private ListView<String> bookListsView;
    @FXML private Label messageLabel;
    @FXML private javafx.scene.control.Button followToggleButton;
    @FXML private javafx.scene.control.Button backButton;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private String viewedUsername;
    private java.util.List<MockBackendService.UserBookList> profileBookLists = java.util.List.of();
    private boolean currentFollowed;

    @FXML
    public void initialize() {
        bookListsView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpenBookList();
            }
        });
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        boolean fromAdminUsers = context.userProfileReturnTarget() == UserProfileReturnTarget.ADMIN_USERS;
        backButton.setVisible(fromAdminUsers);
        backButton.setManaged(fromAdminUsers);
        String target = context.viewedProfileUsername();
        if (target == null || target.isBlank()) {
            target = context.username();
        }
        usernameField.setText(target);
        loadProfile();
    }

    @FXML
    private void onLoadProfile() {
        loadProfile();
    }

    @FXML
    private void onBack() {
        if (context.userProfileReturnTarget() == UserProfileReturnTarget.ADMIN_USERS && mainController != null) {
            context.setUserProfileReturnTarget(UserProfileReturnTarget.NONE);
            mainController.onAdminUsers();
        }
    }

    @FXML
    private void onToggleFollow() {
        try {
            String target = normalizedViewedUsername();
            if (target.isBlank()) {
                messageLabel.setText("请输入要查看的用户名");
                return;
            }
            if (currentFollowed) {
                context.service().unfollow(context.username(), target);
            } else {
                context.service().follow(context.username(), target);
            }
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
            mainController.openBookListDetail(viewedUsername, selected.id(), BookListDetailReturnTarget.USER_PROFILE);
        }
    }

    @FXML
    private void onOpenFollowers() {
        if (viewedUsername == null || viewedUsername.isBlank()) {
            messageLabel.setText("请先加载用户主页");
            return;
        }
        context.setFollowListTargetUsername(viewedUsername);
        context.setShowingFollowers(true);
        if (mainController != null) {
            mainController.openFollowList(viewedUsername, true);
        }
    }

    @FXML
    private void onOpenFollowing() {
        if (viewedUsername == null || viewedUsername.isBlank()) {
            messageLabel.setText("请先加载用户主页");
            return;
        }
        context.setFollowListTargetUsername(viewedUsername);
        context.setShowingFollowers(false);
        if (mainController != null) {
            mainController.openFollowList(viewedUsername, false);
        }
    }

    private void loadProfile() {
        try {
            viewedUsername = normalizedViewedUsername();
            if (viewedUsername.isBlank()) {
                throw new IllegalArgumentException("请输入要查看的用户名");
            }
            MockBackendService.UserProfile profile = context.service().getUserProfile(viewedUsername, context.username());
            viewedUsername = profile.username();
            context.setViewedProfileUsername(viewedUsername);
            usernameField.setText(viewedUsername);
            updateAvatar(profile.avatarImage());
            userLabel.setText("用户：" + profile.username());
            currentFollowed = profile.followedByCurrentUser();
            followToggleButton.setText(currentFollowed ? "取消关注" : "关注");
            relationLabel.setText("关注 " + profile.followingCount() + " | 粉丝 " + profile.followerCount() +
                    " | 我是否已关注：" + (currentFollowed ? "是" : "否"));
            commentsListView.getItems().setAll(profile.comments().stream()
                    .map(this::formatProfileComment)
                    .toList());
            profileBookLists = profile.bookLists();
            bookListsView.getItems().setAll(profile.bookLists().stream()
                    .map(item -> item.title() + "（" + item.bookIds().size() + "本，" +
                            (item.publicVisible() ? "公开" : "私密") + "）")
                    .toList());
            messageLabel.setText(profile.username().equals(context.username())
                    ? "当前查看的是你自己的主页"
                    : "当前查看的是用户 “" + profile.username() + "” 的主页");
        } catch (Exception ex) {
            profileBookLists = java.util.List.of();
            commentsListView.getItems().clear();
            bookListsView.getItems().clear();
            updateAvatar(null);
            messageLabel.setText(ex.getMessage());
        }
    }

    private void updateAvatar(String avatarImage) {
        if (avatarImageView == null) {
            return;
        }
        Image image = ImageDataUtil.image(avatarImage);
        avatarImageView.setImage(image);
        avatarImageView.setVisible(image != null);
        avatarImageView.setManaged(image != null);
    }

    private String normalizedViewedUsername() {
        return usernameField.getText() == null ? "" : usernameField.getText().trim();
    }

    private String formatProfileComment(MockBackendService.CommentItem item) {
        String bookText = item.bookTitle() == null || item.bookTitle().isBlank()
                ? (item.bookId() == null ? "未知图书" : "图书ID " + item.bookId())
                : "《" + item.bookTitle() + "》";
        return bookText + " - " + item.content();
    }
}
