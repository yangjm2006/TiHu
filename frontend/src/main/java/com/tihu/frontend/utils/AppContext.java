package com.tihu.frontend.utils;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.service.RemoteBackendService;
import com.tihu.frontend.service.MockBackendService.Role;
import javafx.stage.Stage;

public final class AppContext {
    public enum BookDetailReturnTarget {
        BOOK_LIST,
        FAVORITES
    }

    private static final AppContext INSTANCE = new AppContext();

    private final MockBackendService service = new RemoteBackendService();
    private Stage primaryStage;
    private String username;
    private Role role;
    private String authToken;
    private Long selectedBookId;
    private Long selectedBookListId;
    private String selectedBookListOwner;
    private String selectedConversationPeer;
    private BookDetailReturnTarget bookDetailReturnTarget = BookDetailReturnTarget.BOOK_LIST;
    private boolean showFollowers;

    private AppContext() {
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public MockBackendService service() {
        return service;
    }

    public Stage primaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public String username() {
        return username;
    }

    public Role role() {
        return role;
    }

    public String authToken() {
        return authToken;
    }

    public boolean isLoggedIn() {
        return username != null;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public void login(String username, Role role) {
        this.username = username;
        this.role = role;
        this.authToken = service.token();
    }

    public void logout() {
        service.logout();
        username = null;
        role = null;
        authToken = null;
        selectedBookId = null;
        selectedBookListId = null;
        selectedBookListOwner = null;
        selectedConversationPeer = null;
        bookDetailReturnTarget = BookDetailReturnTarget.BOOK_LIST;
        showFollowers = false;
    }

    public Long selectedBookId() {
        return selectedBookId;
    }

    public void setSelectedBookId(Long selectedBookId) {
        this.selectedBookId = selectedBookId;
    }

    public Long selectedBookListId() {
        return selectedBookListId;
    }

    public void setSelectedBookListId(Long selectedBookListId) {
        this.selectedBookListId = selectedBookListId;
    }

    public String selectedBookListOwner() {
        return selectedBookListOwner;
    }

    public void setSelectedBookListOwner(String selectedBookListOwner) {
        this.selectedBookListOwner = selectedBookListOwner;
    }

    public String selectedConversationPeer() {
        return selectedConversationPeer;
    }

    public void setSelectedConversationPeer(String selectedConversationPeer) {
        this.selectedConversationPeer = selectedConversationPeer;
    }

    public BookDetailReturnTarget bookDetailReturnTarget() {
        return bookDetailReturnTarget;
    }

    public void setBookDetailReturnTarget(BookDetailReturnTarget bookDetailReturnTarget) {
        this.bookDetailReturnTarget = bookDetailReturnTarget == null ? BookDetailReturnTarget.BOOK_LIST : bookDetailReturnTarget;
    }

    public boolean isShowingFollowers() {
        return showFollowers;
    }

    public void setShowingFollowers(boolean showFollowers) {
        this.showFollowers = showFollowers;
    }
}

