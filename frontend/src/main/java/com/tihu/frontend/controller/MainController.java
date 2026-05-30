package com.tihu.frontend.controller;

import com.tihu.frontend.MainApplication;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.BookDetailReturnTarget;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {

    @FXML private TextField searchField;
    @FXML private StackPane contentPane;
    @FXML private Label userLabel;
    @FXML private Label adminLabel;
    @FXML private VBox adminBox;

    private final AppContext context = AppContext.getInstance();
    private Object currentContentController;

    public Object currentContentController() {
        return currentContentController;
    }

    @FXML
    public void initialize() {
        refreshUserInfo();
        boolean isAdmin = context.isAdmin();
        adminLabel.setVisible(isAdmin);
        adminLabel.setManaged(isAdmin);
        adminBox.setVisible(isAdmin);
        adminBox.setManaged(isAdmin);
        showHome();
    }

    private void setContent(Node view) {
        contentPane.getChildren().setAll(view);
    }

    private void loadContent(String fxml) {
        try {
            java.net.URL location = MainApplication.class.getResource("/com/tihu/frontend/" + fxml);
            if (location == null) {
                throw new IllegalStateException("找不到页面资源：" + fxml);
            }
            FXMLLoader loader = new FXMLLoader(location);
            Node view = loader.load();
            currentContentController = loader.getController();
            if (currentContentController instanceof MainContentController contentController) {
                contentController.setMainController(this);
                try {
                    contentController.onShow();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            String detail = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
                detail = cause.getMessage();
            }
            setContent(new Label("加载页面失败：" + detail));
        }
    }

    public void openBookDetail(long bookId) {
        openBookDetail(bookId, BookDetailReturnTarget.BOOK_LIST);
    }

    public void openBookDetail(long bookId, BookDetailReturnTarget returnTarget) {
        context.setSelectedBookId(bookId);
        context.setBookDetailReturnTarget(returnTarget);
        loadContent("book-detail-view.fxml");
    }

    public void openBookDetail(int bookId) {
        openBookDetail((long) bookId);
    }

    public void openBookListDetail(long listId) {
        openBookListDetail(context.username(), listId);
    }

    public void openBookListDetail(String owner, long listId) {
        context.setSelectedBookListId(listId);
        context.setSelectedBookListOwner(owner);
        loadContent("book-list-detail-view.fxml");
    }

    public void openUserProfile(String username) {
        context.setViewedProfileUsername(username);
        loadContent("user-profile-view.fxml");
    }

    public void openFollowList(String username, boolean showFollowers) {
        context.setFollowListTargetUsername(username);
        context.setShowingFollowers(showFollowers);
        loadContent("following-view.fxml");
    }

    public void openChat(String peer) {
        context.setSelectedConversationPeer(peer);
        loadContent("chat-view.fxml");
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText();
        loadContent("book-list-view.fxml");
        if (currentContentController instanceof BookListController bookListController) {
            bookListController.search(keyword);
        }
    }

    @FXML private void onHome() { showHome(); }
    @FXML public void onBooks() { loadContent("book-list-view.fxml"); }
    @FXML public void onFavorites() { loadContent("favorites-view.fxml"); }
    @FXML public void onBookLists() { loadContent("book-lists-view.fxml"); }
    @FXML public void onUserProfile() { openUserProfile(context.username()); }
    @FXML public void onProfileEdit() { loadContent("profile-edit-view.fxml"); }
    @FXML public void onFollowing() { openFollowList(context.username(), false); }
    @FXML public void onFollowers() { openFollowList(context.username(), true); }
    @FXML public void onConversations() { loadContent("conversations-view.fxml"); }
    @FXML private void onAdminBooks() { loadContent("admin-books-view.fxml"); }
    @FXML private void onAdminUsers() { loadContent("admin-users-view.fxml"); }
    @FXML private void onAdminComments() { loadContent("admin-comments-view.fxml"); }

    @FXML
    private void onLogout() {
        try {
            context.logout();
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 520, 380));
        } catch (Exception ex) {
            setContent(new Label("登出失败：" + ex.getMessage()));
        }
    }

    private void showHome() {
        loadContent("home-view.fxml");
    }

    public void refreshUserInfo() {
        String roleText = context.role() == null ? "未知角色" : context.role().name();
        userLabel.setText("当前用户：" + context.username() + " ｜ 角色：" + roleText);
    }

    private void showPlaceholder(String title) {
        showPlaceholder(title, "该模块后续接入更多功能。");
    }

    private void showPlaceholder(String title, String message) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: #555;");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12, titleLabel, messageLabel);
        box.setStyle("-fx-padding: 24;");
        setContent(box);
    }
}