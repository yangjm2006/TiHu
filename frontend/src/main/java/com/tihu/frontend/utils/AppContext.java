package com.tihu.frontend.utils;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.service.RemoteBackendService;
import com.tihu.frontend.service.MockBackendService.Role;
import javafx.stage.Stage;

public final class AppContext {
    public enum BookDetailReturnTarget {
        BOOK_LIST,
        FAVORITES,
        BOOK_LIST_DETAIL
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
    private String viewedProfileUsername;
    private String followListTargetUsername;
    private BookDetailReturnTarget bookDetailReturnTarget = BookDetailReturnTarget.BOOK_LIST;
    private String bookListTitleKeyword = "";
    private String bookListTagsText = "";
    private int bookListPage = 1;
    private MockBackendService.BookSortMode bookListSortMode = MockBackendService.BookSortMode.DEFAULT;
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
        viewedProfileUsername = null;
        followListTargetUsername = null;
        bookDetailReturnTarget = BookDetailReturnTarget.BOOK_LIST;
        bookListTitleKeyword = "";
        bookListTagsText = "";
        bookListPage = 1;
        bookListSortMode = MockBackendService.BookSortMode.DEFAULT;
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

    public String viewedProfileUsername() {
        return viewedProfileUsername;
    }

    public void setViewedProfileUsername(String viewedProfileUsername) {
        this.viewedProfileUsername = viewedProfileUsername == null ? null : viewedProfileUsername.trim();
    }

    public String followListTargetUsername() {
        return followListTargetUsername;
    }

    public void setFollowListTargetUsername(String followListTargetUsername) {
        this.followListTargetUsername = followListTargetUsername == null ? null : followListTargetUsername.trim();
    }

    public BookDetailReturnTarget bookDetailReturnTarget() {
        return bookDetailReturnTarget;
    }

    public void setBookDetailReturnTarget(BookDetailReturnTarget bookDetailReturnTarget) {
        this.bookDetailReturnTarget = bookDetailReturnTarget == null ? BookDetailReturnTarget.BOOK_LIST : bookDetailReturnTarget;
    }

    public String bookListTitleKeyword() {
        return bookListTitleKeyword;
    }

    public void setBookListTitleKeyword(String bookListTitleKeyword) {
        this.bookListTitleKeyword = bookListTitleKeyword == null ? "" : bookListTitleKeyword;
    }

    public String bookListTagsText() {
        return bookListTagsText;
    }

    public void setBookListTagsText(String bookListTagsText) {
        this.bookListTagsText = bookListTagsText == null ? "" : bookListTagsText;
    }

    public int bookListPage() {
        return bookListPage;
    }

    public void setBookListPage(int bookListPage) {
        this.bookListPage = Math.max(1, bookListPage);
    }

    public MockBackendService.BookSortMode bookListSortMode() {
        return bookListSortMode;
    }

    public void setBookListSortMode(MockBackendService.BookSortMode bookListSortMode) {
        this.bookListSortMode = bookListSortMode == null ? MockBackendService.BookSortMode.DEFAULT : bookListSortMode;
    }

    public boolean isShowingFollowers() {
        return showFollowers;
    }

    public void setShowingFollowers(boolean showFollowers) {
        this.showFollowers = showFollowers;
    }
}

