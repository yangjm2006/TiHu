package com.tihu.frontend.utils;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.service.RemoteBackendService;
import com.tihu.frontend.service.MockBackendService.Role;
import javafx.stage.Stage;

public final class AppContext {
    public enum BookDetailReturnTarget {
        BOOK_LIST,
        FAVORITES,
        BOOK_LIST_DETAIL,
        ADMIN_BOOKS
    }

    public enum BookListDetailReturnTarget {
        BOOK_LISTS,
        USER_PROFILE
    }

    public enum UserProfileReturnTarget {
        NONE,
        ADMIN_USERS
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
    private BookListDetailReturnTarget bookListDetailReturnTarget = BookListDetailReturnTarget.BOOK_LISTS;
    private UserProfileReturnTarget userProfileReturnTarget = UserProfileReturnTarget.NONE;
    private String bookListTitleKeyword = "";
    private String bookListTagsText = "";
    private int bookListPage = 1;
    private MockBackendService.BookSortMode bookListSortMode = MockBackendService.BookSortMode.DEFAULT;
    private String adminBookTitleKeyword = "";
    private String adminBookTagsText = "";
    private int adminBookPage = 1;
    private MockBackendService.BookSortMode adminBookSortMode = MockBackendService.BookSortMode.DEFAULT;
    private Long adminSelectedBookId;
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
        bookListDetailReturnTarget = BookListDetailReturnTarget.BOOK_LISTS;
        userProfileReturnTarget = UserProfileReturnTarget.NONE;
        bookListTitleKeyword = "";
        bookListTagsText = "";
        bookListPage = 1;
        bookListSortMode = MockBackendService.BookSortMode.DEFAULT;
        adminBookTitleKeyword = "";
        adminBookTagsText = "";
        adminBookPage = 1;
        adminBookSortMode = MockBackendService.BookSortMode.DEFAULT;
        adminSelectedBookId = null;
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

    public BookListDetailReturnTarget bookListDetailReturnTarget() {
        return bookListDetailReturnTarget;
    }

    public void setBookListDetailReturnTarget(BookListDetailReturnTarget bookListDetailReturnTarget) {
        this.bookListDetailReturnTarget = bookListDetailReturnTarget == null
                ? BookListDetailReturnTarget.BOOK_LISTS
                : bookListDetailReturnTarget;
    }

    public UserProfileReturnTarget userProfileReturnTarget() {
        return userProfileReturnTarget;
    }

    public void setUserProfileReturnTarget(UserProfileReturnTarget userProfileReturnTarget) {
        this.userProfileReturnTarget = userProfileReturnTarget == null
                ? UserProfileReturnTarget.NONE
                : userProfileReturnTarget;
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

    public String adminBookTitleKeyword() {
        return adminBookTitleKeyword;
    }

    public void setAdminBookTitleKeyword(String adminBookTitleKeyword) {
        this.adminBookTitleKeyword = adminBookTitleKeyword == null ? "" : adminBookTitleKeyword;
    }

    public String adminBookTagsText() {
        return adminBookTagsText;
    }

    public void setAdminBookTagsText(String adminBookTagsText) {
        this.adminBookTagsText = adminBookTagsText == null ? "" : adminBookTagsText;
    }

    public int adminBookPage() {
        return adminBookPage;
    }

    public void setAdminBookPage(int adminBookPage) {
        this.adminBookPage = Math.max(1, adminBookPage);
    }

    public MockBackendService.BookSortMode adminBookSortMode() {
        return adminBookSortMode;
    }

    public void setAdminBookSortMode(MockBackendService.BookSortMode adminBookSortMode) {
        this.adminBookSortMode = adminBookSortMode == null ? MockBackendService.BookSortMode.DEFAULT : adminBookSortMode;
    }

    public Long adminSelectedBookId() {
        return adminSelectedBookId;
    }

    public void setAdminSelectedBookId(Long adminSelectedBookId) {
        this.adminSelectedBookId = adminSelectedBookId;
    }

    public boolean isShowingFollowers() {
        return showFollowers;
    }

    public void setShowingFollowers(boolean showFollowers) {
        this.showFollowers = showFollowers;
    }
}

