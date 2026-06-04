package com.tihu.frontend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tihu.frontend.request.ApiClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RemoteBackendService extends MockBackendService {
    private final ApiClient apiClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean strictRemoteMode = strictRemoteMode();

    private Long currentUserId;
    private String currentUsername;
    private final Map<String, Long> userIdCache = new LinkedHashMap<>();

    public RemoteBackendService() {
        this(new ApiClient());
    }

    public RemoteBackendService(ApiClient apiClient) {
        super();
        this.apiClient = apiClient;
    }

    @Override
    public synchronized Role login(String username, String password) {
        return remoteOrFallbackUnlessApiRejected(() -> remoteLogin(username, password), () -> super.login(username, password));
    }

    @Override
    public synchronized void registerUser(String username, String password) {
        runRemoteOrFallback(() -> requestData("POST", "/users/register", json(Map.of(
                "username", username,
                "password", password
        ))), () -> super.registerUser(username, password));
    }

    @Override
    public synchronized void registerAdmin(String username, String password, String inviteCode) {
        runRemoteOrFallback(() -> requestData("POST", "/users/register" + query(Map.of(
                "inviteCode", inviteCode
        )), json(Map.of(
                "username", username,
                "password", password
        ))), () -> super.registerAdmin(username, password, inviteCode));
    }

    @Override
    public synchronized void updateProfile(String currentUsername, String newUsername, String newPassword) {
        updateProfile(currentUsername, newUsername, newPassword, null);
    }

    @Override
    public synchronized void updateProfile(String currentUsername, String newUsername, String newPassword, String avatarImage) {
        runRemoteOrFallback(() -> remoteUpdateProfile(currentUsername, newUsername, newPassword, avatarImage),
                () -> super.updateProfile(currentUsername, newUsername, newPassword, avatarImage));
    }

    @Override
    public void logout() {
        apiClient.setToken(null);
        apiClient.setSessionCookie(null);
        currentUserId = null;
        currentUsername = null;
        super.logout();
    }

    @Override
    public synchronized Book getBook(long id) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/books/" + id, null);
            return parseBook(firstPresent(data, "bookInfo", "book", "data"), id);
        }, () -> super.getBook(id));
    }

    @Override
    public synchronized Book getBook(int id) {
        return getBook((long) id);
    }

    @Override
    public synchronized BookDetail getBookDetail(long bookId, String currentUser) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/books/" + bookId, null);
            JsonNode bookNode = firstPresent(data, "bookInfo", "book", "data");
            Book book = parseBook(bookNode, bookId);
            List<String> tags = book.tags();
            if (tags.isEmpty()) {
                tags = fetchBookTags(bookId);
                if (!tags.isEmpty()) {
                    book = new Book(book.id(), book.title(), book.author(), book.intro(), tags, book.coverImage());
                }
            }
            RatingSummary summary = parseRatingSummary(firstPresent(data, "ratings", "ratingSummary", "data"), bookId);
            if (summary == null) {
                summary = fetchRatingSummary(bookId);
            }
            List<CommentItem> detailComments = parseComments(firstPresent(data, "comments", "commentList"));
            List<CommentItem> detailReplies = parseComments(firstPresent(data, "replies", "replyList"));
            List<CommentItem> comments = topLevelComments(detailComments);
            List<CommentItem> replies = childComments(detailComments);
            replies = mergeComments(replies, detailReplies);
            if (comments.isEmpty() && replies.isEmpty()) {
                List<CommentItem> allComments = parseComments(requestPageData("/comments/book/" + bookId + query(Map.of("page", 1, "size", 100))));
                comments = topLevelComments(allComments);
                replies = childComments(allComments);
            }
            int favoriteCount = intValue(firstPresent(data, "favoriteCount", "favoritesCount", "collectCount", "collectionCount", "collectedCount"));
            if (favoriteCount <= 0) {
                favoriteCount = fetchFavoriteCount(bookId);
            }
            return new BookDetail(book, summary, comments, replies, favoriteCount);
        }, () -> super.getBookDetail(bookId, currentUser));
    }

    @Override
    public synchronized BookDetail getBookDetail(int bookId, String currentUser) {
        return getBookDetail((long) bookId, currentUser);
    }

    @Override
    public synchronized void rateBook(long bookId, String username, int score) {
        runRemoteOrFallback(() -> requestData("POST", "/ratings" + query(Map.of("bookId", bookId, "score", score)), null),
                () -> super.rateBook(bookId, username, score));
    }

    @Override
    public synchronized void rateBook(int bookId, String username, int score) {
        rateBook((long) bookId, username, score);
    }

    @Override
    public synchronized BookListPage listBooks(String titleKeyword, List<String> tags, int page, int pageSize) {
        return listBooks(titleKeyword, tags, page, pageSize, BookSortMode.DEFAULT);
    }

    public synchronized BookListPage listBooks(String titleKeyword, List<String> tags, int page, int pageSize, BookSortMode sortMode) {
        return remoteOrFallback(() -> {
            if (hasActiveBookSort(sortMode)) {
                return fetchCatalogPageFallback(titleKeyword, tags, page, pageSize, sortMode);
            }
            BookListPage remotePage = fetchBookPage(titleKeyword, tags, page, pageSize, sortMode);
            return remotePage == null ? fetchCatalogPageFallback(titleKeyword, tags, page, pageSize, sortMode) : remotePage;
        }, () -> super.listBooks(titleKeyword, tags, page, pageSize, sortMode));
    }

    @Override
    public synchronized void toggleFavorite(String username, long bookId) {
        runRemoteOrFallback(() -> {
            boolean collected = isCollected(bookId);
            if (collected) {
                requestData("DELETE", "/collections" + query(Map.of("bookId", bookId)), null);
            } else {
                requestData("POST", "/collections" + query(Map.of("bookId", bookId)), null);
            }
        }, () -> super.toggleFavorite(username, bookId));
    }

    @Override
    public synchronized void toggleFavorite(String username, int bookId) {
        toggleFavorite(username, (long) bookId);
    }

    @Override
    public synchronized boolean isCollected(String username, long bookId) {
        return remoteOrFallback(() -> isCollected(bookId), () -> super.isCollected(username, bookId));
    }

    @Override
    public synchronized boolean isCollected(String username, int bookId) {
        return isCollected(username, (long) bookId);
    }


    @Override
    public synchronized void updateBook(long bookId, String title, String author, String intro, String tagsText) {
        updateBook(bookId, title, author, intro, tagsText, null);
    }

    @Override
    public synchronized void updateBook(long bookId, String title, String author, String intro, String tagsText, String coverImage) {
        runRemoteOrFallback(() -> {
            List<String> tags = parseTagInput(tagsText);
            requestData("PUT", "/books/" + bookId, json(mapOf(
                    "title", title,
                    "author", author,
                    "description", intro,
                    "cover", coverImage,
                    "coverImage", coverImage,
                    "coverUrl", coverImage,
                    "tags", tags
            )));
        }, () -> super.updateBook(bookId, title, author, intro, tagsText, coverImage));
    }

    @Override
    public synchronized void updateBook(int bookId, String title, String author, String intro, String tagsText) {
        updateBook((long) bookId, title, author, intro, tagsText);
    }

    @Override
    public synchronized void updateBook(int bookId, String title, String author, String intro, String tagsText, String coverImage) {
        updateBook((long) bookId, title, author, intro, tagsText, coverImage);
    }

    @Override
    public synchronized Book addBook(String title, String author, String intro, String tagsText) {
        return addBook(title, author, intro, tagsText, null);
    }

    @Override
    public synchronized Book addBook(String title, String author, String intro, String tagsText, String coverImage) {
        return remoteOrFallback(() -> {
            List<String> tags = parseTagInput(tagsText);
            JsonNode data = requestData("POST", "/books", json(mapOf(
                    "title", title,
                    "author", author,
                    "description", intro,
                    "cover", coverImage,
                    "coverImage", coverImage,
                    "coverUrl", coverImage,
                    "tags", tags
            )));
            Book book = parseBook(firstPresent(data, "book", "data", "bookInfo"), -1L);
            if (book != null) {
                return book;
            }
            return super.addBook(title, author, intro, tagsText, coverImage);
        }, () -> super.addBook(title, author, intro, tagsText, coverImage));
    }

    @Override
    public synchronized void deleteBook(long bookId) {
        runRemoteOrFallback(() -> requestData("DELETE", "/books/" + bookId, null), () -> super.deleteBook(bookId));
    }

    @Override
    public synchronized void deleteBook(int bookId) {
        deleteBook((long) bookId);
    }

    @Override
    public synchronized CommentItem addComment(long bookId, String username, String content, Long parentCommentId) {
        return remoteOrFallback(() -> {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("bookId", bookId);
            params.put("content", content);
            if (parentCommentId != null) {
                params.put("parentCommentId", parentCommentId);
            }
            JsonNode data = requestData("POST", "/comments" + query(params), null);
            CommentItem item = parseCommentItem(firstPresent(data, "comment", "data"));
            if (item == null) {
                item = parseCommentItem(data);
            }
            return item != null ? item : super.addComment(bookId, username, content, parentCommentId);
        }, () -> super.addComment(bookId, username, content, parentCommentId));
    }

    @Override
    public synchronized CommentItem addComment(int bookId, String username, String content, Long parentCommentId) {
        return addComment((long) bookId, username, content, parentCommentId);
    }

    @Override
    public synchronized void deleteOwnComment(long bookId, long commentId, String username) {
        runRemoteOrFallbackUnlessApiRejected(() -> requestData("DELETE", "/comments/" + commentId, null),
                () -> super.deleteOwnComment(bookId, commentId, username));
    }

    @Override
    public synchronized void deleteOwnComment(int bookId, long commentId, String username) {
        deleteOwnComment((long) bookId, commentId, username);
    }

    @Override
    public synchronized void adminDeleteComment(long bookId, long commentId) {
        runRemoteOrFallbackUnlessApiRejected(() -> requestData("DELETE", "/comments/admin/" + commentId, null),
                () -> super.adminDeleteComment(bookId, commentId));
    }

    @Override
    public synchronized void adminDeleteComment(int bookId, long commentId) {
        adminDeleteComment((long) bookId, commentId);
    }

    @Override
    public synchronized void adminDeleteComment(long commentId) {
        runRemoteOrFallbackUnlessApiRejected(() -> requestData("DELETE", "/comments/admin/" + commentId, null),
                () -> super.adminDeleteComment(commentId));
    }

    @Override
    public synchronized void voteComment(long commentId, String username, int target) {
        runRemoteOrFallback(() -> requestData("POST", "/comments/" + commentId + "/votes" + query(Map.of("target", target)), null),
                () -> super.voteComment(commentId, username, target));
    }

    @Override
    public synchronized List<UserBookList> listBookLists(String username) {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/book-lists" + query(Map.of("page", 1, "size", 1000)));
            List<UserBookList> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                UserBookList list = parseBookList(item);
                if (list != null && Objects.equals(list.owner(), username)) {
                    result.add(list);
                }
            }
            return result;
        }, () -> super.listBookLists(username));
    }

    @Override
    public synchronized UserBookList createBookList(String username, String title, String intro) {
        return createBookList(username, title, intro, true);
    }

    @Override
    public synchronized UserBookList createBookList(String username, String title, String intro, boolean publicVisible) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("POST", "/book-lists" + query(mapOf(
                    "title", title,
                    "description", intro,
                    "publicVisible", publicVisible,
                    "visibility", publicVisible ? "PUBLIC" : "PRIVATE"
            )), null);
            UserBookList list = parseBookList(firstPresent(data, "bookList", "data", "list"));
            return list != null ? list : super.createBookList(username, title, intro, publicVisible);
        }, () -> super.createBookList(username, title, intro, publicVisible));
    }

    @Override
    public synchronized void updateBookListVisibility(String username, long listId, boolean publicVisible) {
        runRemoteOrFallback(() -> requestData("PUT", "/book-lists/" + listId + "/visibility" + query(mapOf(
                "publicVisible", publicVisible,
                "visibility", publicVisible ? "PUBLIC" : "PRIVATE"
        )), null), () -> super.updateBookListVisibility(username, listId, publicVisible));
    }

    @Override
    public synchronized void deleteBookList(String username, long listId) {
        runRemoteOrFallback(() -> requestData("DELETE", "/book-lists/" + listId, null),
                () -> super.deleteBookList(username, listId));
    }

    @Override
    public synchronized UserBookList getBookList(String owner, long listId) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/book-lists/" + listId, null);
            UserBookList list = parseBookList(firstPresent(data, "bookList", "data", "list"));
            if (list == null) {
                list = parseBookList(data);
            }
            return list != null ? list : super.getBookList(owner, listId);
        }, () -> super.getBookList(owner, listId));
    }

    @Override
    public synchronized void addBookToBookList(String username, long listId, long bookId) {
        runRemoteOrFallback(() -> requestData("POST", "/book-lists/" + listId + "/books" + query(Map.of("bookId", bookId)), null),
                () -> super.addBookToBookList(username, listId, bookId));
    }

    @Override
    public synchronized void addBookToBookList(String username, long listId, int bookId) {
        addBookToBookList(username, listId, (long) bookId);
    }

    @Override
    public synchronized void addBookToBookList(String username, long listId, String bookTitle) {
        runRemoteOrFallback(() -> requestData("POST", "/book-lists/" + listId + "/books" + query(Map.of("bookTitle", bookTitle)), null),
                () -> {
                    long bookId = resolveBookIdByTitle(bookTitle);
                    addBookToBookList(username, listId, bookId);
                });
    }

    @Override
    public synchronized void removeBookFromBookList(String username, long listId, long bookId) {
        runRemoteOrFallback(() -> requestData("DELETE", "/book-lists/" + listId + "/books" + query(Map.of("bookId", bookId)), null),
                () -> super.removeBookFromBookList(username, listId, bookId));
    }

    @Override
    public synchronized void removeBookFromBookList(String username, long listId, int bookId) {
        removeBookFromBookList(username, listId, (long) bookId);
    }

    @Override
    public synchronized void follow(String me, String target) {
        runRemoteOrFallback(() -> requestData("POST", "/follows" + resolveFollowQuery(target), null),
                () -> super.follow(me, target));
    }

    @Override
    public synchronized void unfollow(String me, String target) {
        runRemoteOrFallback(() -> requestData("DELETE", "/follows" + resolveFollowQuery(target), null),
                () -> super.unfollow(me, target));
    }

    @Override
    public synchronized List<FollowItem> listFollowing(String username) {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData(resolveFollowListPath(username, false));
            List<FollowItem> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                FollowItem follow = parseFollowItem(item);
                if (follow != null) {
                    result.add(follow);
                }
            }
            return result;
        }, () -> super.listFollowing(username));
    }

    @Override
    public synchronized List<FollowItem> listFollowers(String username) {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData(resolveFollowListPath(username, true));
            List<FollowItem> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                FollowItem follow = parseFollowItem(item);
                if (follow != null) {
                    result.add(follow);
                }
            }
            return result;
        }, () -> super.listFollowers(username));
    }

    @Override
    public synchronized UserProfile getUserProfile(String target, String currentUser) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/users/profile/" + target, null);
            UserProfile profile = parseUserProfile(data, target);
            return profile != null ? profile : super.getUserProfile(target, currentUser);
        }, () -> super.getUserProfile(target, currentUser));
    }

    @Override
    public synchronized void sendMessage(String from, String to, String content) {
        runRemoteOrFallback(() -> requestData("POST", "/messages" + resolveMessageQuery(to, content), null),
                () -> super.sendMessage(from, to, content));
    }

    @Override
    public synchronized List<ConversationPreview> listConversations(String username) {
        return remoteOrFallback(() -> {
            JsonNode data = requestPageData("/messages/conversations");
            List<ConversationPreview> result = new ArrayList<>();
            for (JsonNode item : pageRecords(data)) {
                ConversationPreview preview = parseConversationPreview(item);
                if (preview != null) {
                    result.add(preview);
                }
            }
            return result;
        }, () -> super.listConversations(username));
    }

    @Override
    public synchronized List<MessageItem> listMessages(String me, String peer) {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/messages/conversation/" + resolveConversationPeer(peer) + query(Map.of("page", 1, "size", 100)));
            List<MessageItem> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                MessageItem message = parseMessageItem(item);
                if (message != null) {
                    result.add(message);
                }
            }
            return result;
        }, () -> super.listMessages(me, peer));
    }

    @Override
    public synchronized List<BanInfo> listBans() {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/users/bans" + query(Map.of("page", 1, "size", 1000)));
            List<BanInfo> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                BanInfo info = parseBanInfo(item);
                if (info != null) {
                    result.add(info);
                }
            }
            return result;
        }, super::listBans);
    }

    @Override
    public synchronized List<AdminUserInfo> listAdminUsers() {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/users/admin" + query(Map.of(
                    "page", 1,
                    "size", 1000,
                    "sort", "created_at_asc"
            )));
            List<AdminUserInfo> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                AdminUserInfo info = parseAdminUserInfo(item);
                if (info != null) {
                    result.add(info);
                }
            }
            return result.stream()
                    .sorted(Comparator.comparing(AdminUserInfo::createdAt,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }, super::listAdminUsers);
    }

    @Override
    public synchronized void banUser(String username, LocalDateTime until) {
        runRemoteOrFallback(() -> requestData("POST", "/users/ban" + query(mapOf("username", username, "until", until == null ? null : until.toString())), null),
                () -> super.banUser(username, until));
    }

    @Override
    public synchronized void unbanUser(String username) {
        runRemoteOrFallback(() -> requestData("POST", "/users/unban" + query(Map.of("username", username)), null),
                () -> super.unbanUser(username));
    }

    @Override
    public synchronized void grantAdmin(String username) {
        runRemoteOrFallback(() -> requestData("POST", "/users/grant-admin" + query(Map.of("username", username)), null),
                () -> super.grantAdmin(username));
    }

    @Override
    public synchronized List<CommentItem> adminListAllComments() {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/comments/admin" + query(Map.of("page", 1, "size", 1000)));
            List<CommentItem> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                CommentItem comment = parseCommentItem(item);
                if (comment != null) {
                    result.add(comment);
                }
            }
            return result;
        }, super::adminListAllComments);
    }

    @Override
    public synchronized List<BookCard> listFavorites(String username) {
        return listFavoriteItems(username).stream().map(FavoriteItem::book).toList();
    }

    @Override
    public synchronized List<FavoriteItem> listFavoriteItems(String username) {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/collections" + query(Map.of("page", 1, "size", 1000)));
            List<FavoriteItem> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                FavoriteItem favorite = parseFavoriteItem(item, username);
                if (favorite != null) {
                    result.add(favorite);
                }
            }
            return result;
        }, () -> super.listFavoriteItems(username));
    }

    private List<BookCard> fetchCatalog(String titleKeyword, List<String> tags, BookSortMode sortMode) throws IOException, InterruptedException {
        boolean needTags = tags != null && !tags.isEmpty();
        String sortParam = sortParam(sortMode);
        JsonNode page = fetchCatalogData(needTags, tags, sortParam);
        List<BookCard> result = new ArrayList<>();
        for (JsonNode item : pageRecords(page)) {
            BookCard card = parseBookCard(item);
            if (card == null) {
                continue;
            }
            if (containsKeyword(card.title(), titleKeyword)) {
                result.add(card);
            }
        }
        if (needTags && !tags.isEmpty()) {
            List<String> lowerNeed = tags.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
            result = result.stream()
                    .filter(card -> {
                        List<String> cardTags = parseStringListFromSummary(card.tagsSummary());
                        if (cardTags.isEmpty()) {
                            return false;
                        }
                        return cardTags.stream()
                                .map(v -> v.toLowerCase(Locale.ROOT))
                                .collect(Collectors.toSet())
                                .containsAll(lowerNeed);
                    })
                    .toList();
        }
        return applySortMode(result, sortMode);
    }

    private JsonNode fetchCatalogData(boolean needTags, List<String> tags, String sortParam) throws IOException, InterruptedException {
        if (needTags) {
            try {
                return requestPageData("/books/search-by-tags" + query(mapOf("tags", tags, "sort", sortParam, "page", 1, "size", 1000)));
            } catch (Exception ignore) {
            }
        }
        return requestPageData("/books" + query(Map.of("page", 1, "size", 1000, "sort", sortParam)));
    }

    private BookListPage fetchBookPage(String titleKeyword, List<String> tags, int page, int pageSize,
                                       BookSortMode sortMode) throws IOException, InterruptedException {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        List<String> normalizedTags = tags == null ? List.of() : tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .toList();
        Map<String, Object> params = mapOf(
                "page", safePage,
                "size", safePageSize,
                "sort", sortParam(sortMode),
                "keyword", titleKeyword,
                "title", titleKeyword,
                "tags", normalizedTags.isEmpty() ? null : normalizedTags
        );
        JsonNode pageData = requestPageData("/books" + query(params));
        List<BookCard> items = new ArrayList<>();
        for (JsonNode item : pageRecords(pageData)) {
            BookCard card = parseBookCard(item);
            if (card != null) {
                items.add(card);
            }
        }
        if (hasActiveBookFilter(titleKeyword, normalizedTags) && !bookPageMatchesFilters(items, titleKeyword, normalizedTags)) {
            return null;
        }
        if (hasActiveBookSort(sortMode) && !bookPageMatchesSort(items, sortMode)) {
            return null;
        }
        if (!hasPageMetadata(pageData)) {
            return null;
        }
        int totalItems = pageInt(pageData, items.size(), "total", "totalItems", "totalElements", "count");
        int totalPages = pageInt(pageData, Math.max(1, (int) Math.ceil(totalItems / (double) safePageSize)),
                "pages", "totalPages");
        int current = pageInt(pageData, safePage, "current", "page", "pageNumber");
        return new BookListPage(items, Math.max(1, current), Math.max(1, totalPages), Math.max(0, totalItems));
    }

    private boolean hasActiveBookFilter(String titleKeyword, List<String> tags) {
        return titleKeyword != null && !titleKeyword.isBlank() || tags != null && !tags.isEmpty();
    }

    private boolean hasActiveBookSort(BookSortMode sortMode) {
        return sortMode != null && sortMode != BookSortMode.DEFAULT;
    }

    private boolean bookPageMatchesFilters(List<BookCard> items, String titleKeyword, List<String> tags) {
        return items.stream().allMatch(item ->
                containsKeyword(item.title(), titleKeyword)
                        && containsAllBookTags(parseStringListFromSummary(item.tagsSummary()), tags));
    }

    private boolean bookPageMatchesSort(List<BookCard> items, BookSortMode sortMode) {
        if (items == null || items.size() <= 1 || !hasActiveBookSort(sortMode)) {
            return true;
        }
        for (int i = 1; i < items.size(); i++) {
            if (compareBookCards(items.get(i - 1), items.get(i), sortMode) > 0) {
                return false;
            }
        }
        return true;
    }

    private int compareBookCards(BookCard left, BookCard right, BookSortMode sortMode) {
        return switch (sortMode) {
            case RATING_DESC -> {
                int cmp = compareRatingTextDesc(left.averageScoreText(), right.averageScoreText());
                if (cmp != 0) {
                    yield cmp;
                }
                cmp = left.title().compareToIgnoreCase(right.title());
                yield cmp != 0 ? cmp : Long.compare(left.id(), right.id());
            }
            case TITLE_ASC -> {
                int cmp = left.title().compareToIgnoreCase(right.title());
                yield cmp != 0 ? cmp : Long.compare(left.id(), right.id());
            }
            case DEFAULT -> 0;
        };
    }

    private boolean containsAllBookTags(List<String> sourceTags, List<String> needTags) {
        if (needTags == null || needTags.isEmpty()) {
            return true;
        }
        List<String> source = sourceTags == null ? List.of() : sourceTags.stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .toList();
        return needTags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .allMatch(source::contains);
    }

    private BookListPage fetchCatalogPageFallback(String titleKeyword, List<String> tags, int page, int pageSize,
                                                  BookSortMode sortMode) throws IOException, InterruptedException {
        List<BookCard> catalog = fetchCatalog(titleKeyword, tags, sortMode);
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int totalItems = catalog.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safePageSize));
        int from = Math.min((safePage - 1) * safePageSize, totalItems);
        int to = Math.min(from + safePageSize, totalItems);
        return new BookListPage(new ArrayList<>(catalog.subList(from, to)), safePage, totalPages, totalItems);
    }

    private List<BookCard> applySortMode(List<BookCard> cards, BookSortMode sortMode) {
        if (cards == null || cards.size() <= 1 || sortMode == null || sortMode == BookSortMode.DEFAULT) {
            return cards == null ? List.of() : cards;
        }
        return switch (sortMode) {
            case RATING_DESC -> cards.stream()
                    .sorted((a, b) -> {
                        int cmp = compareRatingTextDesc(a.averageScoreText(), b.averageScoreText());
                        if (cmp != 0) {
                            return cmp;
                        }
                        cmp = a.title().compareToIgnoreCase(b.title());
                        if (cmp != 0) {
                            return cmp;
                        }
                        return Long.compare(a.id(), b.id());
                    })
                    .toList();
            case TITLE_ASC -> cards.stream()
                    .sorted((a, b) -> {
                        int cmp = a.title().compareToIgnoreCase(b.title());
                        if (cmp != 0) {
                            return cmp;
                        }
                        return Long.compare(a.id(), b.id());
                    })
                    .toList();
            case DEFAULT -> cards;
        };
    }

    private String sortParam(BookSortMode sortMode) {
        return switch (sortMode == null ? BookSortMode.DEFAULT : sortMode) {
            case RATING_DESC -> "rating_desc";
            case TITLE_ASC -> "title_asc";
            case DEFAULT -> "default";
        };
    }

    private int compareRatingTextDesc(String left, String right) {
        return Double.compare(parseRatingText(right), parseRatingText(left));
    }

    private double parseRatingText(String value) {
        if (value == null || value.isBlank() || value.contains("暂无评分")) {
            return -1;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    private BookCard parseBookCard(JsonNode item) {
        if (item == null || item.isMissingNode() || item.isNull()) {
            return null;
        }
        JsonNode bookNode = firstPresent(item, "bookInfo", "book", "data");
        long bookId = longValue(firstPresent(item, "id", "bookId", "book_id"), -1L);
        if (bookId <= 0) {
            bookId = longValue(firstPresent(bookNode, "id", "bookId", "book_id"), -1L);
        }
        if (bookId <= 0) {
            return null;
        }
        String title = text(firstPresent(item, "title", "bookTitle"));
        if (title == null || title.isBlank()) {
            title = text(firstPresent(bookNode, "title", "bookTitle"));
        }
        String author = text(firstPresent(item, "author", "bookAuthor"));
        if (author == null || author.isBlank()) {
            author = text(firstPresent(bookNode, "author", "bookAuthor"));
        }
        List<String> tags = parseStringList(firstPresent(item, "tags", "tagNames", "tagList"));
        if (tags.isEmpty()) {
            tags = parseStringList(firstPresent(bookNode, "tags", "tagNames", "tagList"));
        }
        if (tags.isEmpty()) {
            tags = parseStringListFromSummary(text(firstPresent(item, "tagsSummary", "tags_summary")));
        }
        if (tags.isEmpty()) {
            tags = fetchBookTags(bookId);
        }
        RatingSummary summary = parseRatingSummary(item, bookId);
        JsonNode nestedRatings = firstPresent(item, "ratings", "ratingSummary");
        if (nestedRatings != null) {
            RatingSummary nestedSummary = parseRatingSummary(nestedRatings, bookId);
            if (nestedSummary != null && (summary == null || nestedSummary.count() > 0)) {
                summary = nestedSummary;
            }
        }
        if (summary == null || summary.count() == 0) {
            summary = fetchRatingSummary(bookId);
        }
        if (title == null) {
            title = "";
        }
        if (author == null) {
            author = "";
        }
        return new BookCard(bookId, title, author, String.join(" ", tags), formatAvg(summary));
    }

    private BookCard parseFavoriteCard(JsonNode item, String username) {
        if (item == null || item.isMissingNode() || item.isNull()) {
            return null;
        }
        String owner = safe(text(firstPresent(item, "owner", "username", "user", "nickname")));
        if (!owner.isBlank() && username != null && !username.isBlank() && !owner.equals(username)) {
            return null;
        }
        BookCard card = parseBookCard(item);
        if (card != null) {
            return card;
        }

        JsonNode bookNode = firstPresent(item, "bookInfo", "book", "data");
        long bookId = longValue(firstPresent(item, "bookId", "book_id", "id"), -1L);
        if (bookId <= 0 && bookNode != null) {
            bookId = longValue(firstPresent(bookNode, "id", "bookId", "book_id"), -1L);
        }
        if (bookId <= 0) {
            return null;
        }

        String title = safe(text(firstPresent(item, "title", "bookTitle")));
        String author = safe(text(firstPresent(item, "author", "bookAuthor")));
        List<String> tags = parseStringList(firstPresent(item, "tags", "tagNames", "tagList"));
        if (tags.isEmpty() && bookNode != null) {
            tags = parseStringList(firstPresent(bookNode, "tags", "tagNames", "tagList"));
        }
        if (tags.isEmpty()) {
            tags = parseStringListFromSummary(text(firstPresent(item, "tagsSummary", "tags_summary")));
        }
        if (title.isBlank() || author.isBlank()) {
            try {
                Book book = getBook(bookId);
                if (title.isBlank()) {
                    title = book.title();
                }
                if (author.isBlank()) {
                    author = book.author();
                }
                if (tags.isEmpty()) {
                    tags = book.tags();
                }
            } catch (Exception ignore) {
            }
        }

        RatingSummary summary = parseRatingSummary(item, bookId);
        if (summary == null || summary.count() == 0) {
            summary = fetchRatingSummary(bookId);
        }
        return new BookCard(bookId, title, author, String.join(" ", tags), formatAvg(summary));
    }

    private FavoriteItem parseFavoriteItem(JsonNode item, String username) {
        BookCard card = parseFavoriteCard(item, username);
        if (card == null) {
            return null;
        }
        LocalDateTime collectedAt = parseDateTime(text(firstPresent(item, "collectedAt", "collectTime",
                "collectionTime", "createTime", "createdAt", "time")));
        return new FavoriteItem(card, collectedAt);
    }

    private Book parseBook(JsonNode node, long fallbackId) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        long id = longValue(firstPresent(node, "id", "bookId", "book_id"), fallbackId);
        String title = safe(text(firstPresent(node, "title", "bookTitle")));
        String author = safe(text(firstPresent(node, "author", "bookAuthor")));
        String intro = safe(text(firstPresent(node, "description", "intro")));
        List<String> tags = parseStringList(firstPresent(node, "tags"));
        if (tags.isEmpty()) {
            tags = parseStringListFromSummary(text(firstPresent(node, "tagsSummary", "tags_summary")));
        }
        if (tags.isEmpty()) {
            JsonNode bookInfo = firstPresent(node, "bookInfo");
            if (bookInfo != null && bookInfo != node) {
                tags = parseStringList(firstPresent(bookInfo, "tags"));
                if (tags.isEmpty()) {
                    tags = parseStringListFromSummary(text(firstPresent(bookInfo, "tagsSummary", "tags_summary")));
                }
            }
        }
        if (tags.isEmpty() && id > 0) {
            tags = fetchBookTags(id);
        }
        String coverImage = safe(text(firstPresent(node, "coverImage", "cover", "coverUrl", "image", "imageUrl")));
        if (coverImage.isBlank()) {
            JsonNode bookInfo = firstPresent(node, "bookInfo", "book");
            if (bookInfo != null && bookInfo != node) {
                coverImage = safe(text(firstPresent(bookInfo, "coverImage", "cover", "coverUrl", "image", "imageUrl")));
            }
        }
        return new Book(id, title, author, intro, tags, coverImage.isBlank() ? null : coverImage);
    }

    private List<String> fetchBookTags(long bookId) {
        try {
            JsonNode data = requestData("GET", "/books/" + bookId + "/tags", null);
            JsonNode payload = firstPresent(data, "data", "tags", "tagList", "tagNames");
            if (payload != null && payload.isArray()) {
                return parseStringList(payload);
            }
            if (payload != null && payload.isObject()) {
                return parseStringList(firstPresent(payload, "tags", "tagList", "tagNames"));
            }
            if (data != null && data.isArray()) {
                return parseStringList(data);
            }
            return parseStringList(payload);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> parseStringListFromSummary(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.trim().split("\\s+"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private List<String> parseTagInput(String tagsText) {
        if (tagsText == null || tagsText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tagsText.trim().split("\\s+"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private boolean isCollected(long bookId) {
        try {
            JsonNode data = requestData("GET", "/collections/check" + query(Map.of("bookId", bookId)), null);
            JsonNode value = firstPresent(data, "data", "result", "value", "collected");
            return value != null && (value.isBoolean() ? value.asBoolean() : Boolean.parseBoolean(value.asText()));
        } catch (Exception ex) {
            return false;
        }
    }

    private RatingSummary fetchRatingSummary(long bookId) {
        try {
            JsonNode data = requestData("GET", "/ratings/book/" + bookId + "/stats", null);
            return parseRatingSummary(data, bookId);
        } catch (Exception ex) {
            return null;
        }
    }

    private int fetchFavoriteCount(long bookId) {
        try {
            JsonNode page = requestPageData("/collections" + query(Map.of("page", 1, "size", 1000)));
            int count = 0;
            for (JsonNode item : pageRecords(page)) {
                if (matchesBookId(item, bookId)) {
                    count++;
                }
            }
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }

    private boolean matchesBookId(JsonNode node, long bookId) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        Long value = longNullable(firstPresent(node, "bookId", "book_id", "id"));
        if (value != null && value == bookId) {
            return true;
        }
        JsonNode bookInfo = firstPresent(node, "bookInfo", "book", "data");
        if (bookInfo != null && bookInfo != node) {
            Long nested = longNullable(firstPresent(bookInfo, "bookId", "book_id", "id"));
            return nested != null && nested == bookId;
        }
        return false;
    }

    private RatingSummary parseRatingSummary(JsonNode node, long bookId) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        double avg = doubleValue(firstPresent(node, "average", "avg", "avgScore", "averageScore"));
        int count = intValue(firstPresent(node, "count", "ratingCount", "total"));
        Map<Integer, Integer> distribution = defaultDistribution();
        JsonNode distNode = firstPresent(node, "distribution", "ratingDistribution");
        if (distNode != null && distNode.isObject()) {
            for (int i = 1; i <= 10; i++) {
                distribution.put(i, intValue(distNode.get(String.valueOf(i))));
            }
        }
        Integer myScore = null;
        try {
            JsonNode my = requestData("GET", "/ratings/my" + query(Map.of("bookId", bookId)), null);
            myScore = intNullable(firstPresent(my, "score", "myScore", "data"));
        } catch (Exception ignore) {
        }
        return new RatingSummary(avg, count, distribution, myScore);
    }

    private List<CommentItem> parseComments(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        List<CommentItem> result = new ArrayList<>();
        JsonNode data = node.isArray() ? node : firstPresent(node, "records", "list", "data", "comments");
        if (data != null && data.isArray()) {
            for (JsonNode item : data) {
                CommentItem comment = parseCommentItem(item);
                if (comment == null) {
                    continue;
                }
                result.add(comment);
                JsonNode replies = firstPresent(item, "replies", "replyList", "children");
                for (CommentItem reply : parseComments(replies)) {
                    result.add(reply.parentId() == null
                            ? new CommentItem(reply.id(), reply.user(), reply.content(), reply.time(), comment.id(),
                            reply.upVotes(), reply.downVotes(), reply.bookId(), reply.bookTitle())
                            : reply);
                }
            }
        }
        return result;
    }

    private long resolveBookIdByTitle(String bookTitle) {
        String normalized = bookTitle == null ? "" : bookTitle.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("图书名称不能为空");
        }
        List<BookCard> cards = listBooks(normalized, List.of(), 1, 1000).items();
        List<BookCard> exactMatches = cards.stream()
                .filter(card -> card.title().equalsIgnoreCase(normalized))
                .toList();
        if (exactMatches.size() == 1) {
            return exactMatches.getFirst().id();
        }
        if (exactMatches.size() > 1) {
            throw new IllegalStateException("存在多本同名图书，请在图书详情页加入书单");
        }
        if (cards.size() == 1) {
            return cards.getFirst().id();
        }
        if (cards.isEmpty()) {
            return super.findBookByTitle(normalized).id();
        }
        throw new IllegalStateException("匹配到多本图书，请输入完整书名");
    }

    private List<CommentItem> topLevelComments(List<CommentItem> comments) {
        return comments.stream().filter(item -> item.parentId() == null).toList();
    }

    private List<CommentItem> childComments(List<CommentItem> comments) {
        return comments.stream().filter(item -> item.parentId() != null).toList();
    }

    private List<CommentItem> mergeComments(List<CommentItem> first, List<CommentItem> second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        Map<Long, CommentItem> merged = new LinkedHashMap<>();
        first.forEach(item -> merged.put(item.id(), item));
        second.forEach(item -> merged.putIfAbsent(item.id(), item));
        return new ArrayList<>(merged.values());
    }

    private List<JsonNode> pageRecords(JsonNode page) {
        if (page == null || page.isMissingNode() || page.isNull()) {
            return List.of();
        }
        JsonNode records = firstPresent(page, "records", "list", "items", "data");
        if (records != null && records.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            records.forEach(result::add);
            return result;
        }
        if (page.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            page.forEach(result::add);
            return result;
        }
        return List.of();
    }

    private boolean hasPageMetadata(JsonNode page) {
        return firstPresent(page, "total", "totalItems", "totalElements", "count", "pages", "totalPages",
                "current", "page", "pageNumber", "size") != null;
    }

    private int pageInt(JsonNode page, int defaultValue, String... names) {
        JsonNode node = firstPresent(page, names);
        return node == null ? defaultValue : Math.max(0, node.asInt(defaultValue));
    }

    private JsonNode requestPageData(String path) throws IOException, InterruptedException {
        return requestJson("GET", path, null);
    }

    private JsonNode requestData(String method, String path, String body) throws IOException, InterruptedException {
        return requestJson(method, path, body);
    }

    private JsonNode requestJson(String method, String path, String body) throws IOException, InterruptedException {
        String response = apiClient.send(method, path, body);
        JsonNode node = mapper.readTree(response);
        JsonNode codeNode = firstPresent(node, "code", "status");
        if (codeNode != null && codeNode.isNumber() && codeNode.asInt() != 200) {
            String message = text(firstPresent(node, "message", "msg", "errorMessage"));
            JsonNode data = firstPresent(node, "data", "result");
            throw new RemoteApiException(formatRemoteErrorMessage(message, data));
        }
        JsonNode data = firstPresent(node, "data", "result");
        return data == null ? node : data;
    }

    private Role remoteLogin(String username, String password) throws IOException, InterruptedException {
        JsonNode data = requestData("POST", "/users/login", json(Map.of(
                "username", username,
                "password", password
        )));
        JsonNode userInfo = firstPresent(data, "userInfo", "user", "data");
        if (userInfo == null) {
            userInfo = data;
        }
        currentUserId = longNullable(firstPresent(userInfo, "id", "userId"));
        currentUsername = safe(text(firstPresent(userInfo, "username", "name")));
        Role role = parseRole(text(firstPresent(userInfo, "role", "userRole")));
        if (role == null) {
            role = parseRole(text(firstPresent(data, "role")));
        }
        if (role == null) {
            throw new IllegalStateException("登录响应缺少角色信息");
        }
        return role;
    }

    private void remoteUpdateProfile(String currentUsername, String newUsername, String newPassword, String avatarImage) throws IOException, InterruptedException {
        JsonNode payload = requestData("PUT", "/users/profile", json(mapOf(
                "currentUsername", currentUsername,
                "newUsername", newUsername,
                "newPassword", newPassword,
                "avatarImage", avatarImage,
                "avatar", avatarImage,
                "avatarUrl", avatarImage
        )));
        if (payload == null) {
            requestData("PUT", "/users/me", json(mapOf(
                    "username", currentUsername,
                    "newUsername", newUsername,
                    "newPassword", newPassword,
                    "avatarImage", avatarImage,
                    "avatar", avatarImage,
                    "avatarUrl", avatarImage
            )));
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private <T> T remoteOrFallback(ThrowingSupplier<T> remote, Supplier<T> fallback) {
        try {
            return remote.get();
        } catch (Exception ex) {
            if (strictRemoteMode) {
                throw remoteFailure(ex);
            }
            return fallback.get();
        }
    }

    private <T> T remoteOrFallbackUnlessApiRejected(ThrowingSupplier<T> remote, Supplier<T> fallback) {
        try {
            return remote.get();
        } catch (RemoteApiException ex) {
            throw ex;
        } catch (Exception ex) {
            if (strictRemoteMode) {
                throw remoteFailure(ex);
            }
            return fallback.get();
        }
    }

    private void runRemoteOrFallback(ThrowingRunnable remote, Runnable fallback) {
        try {
            remote.run();
        } catch (Exception ex) {
            if (strictRemoteMode) {
                throw remoteFailure(ex);
            }
            fallback.run();
        }
    }

    private void runRemoteOrFallbackUnlessApiRejected(ThrowingRunnable remote, Runnable fallback) {
        try {
            remote.run();
        } catch (RemoteApiException ex) {
            throw ex;
        } catch (Exception ex) {
            if (strictRemoteMode) {
                throw remoteFailure(ex);
            }
            fallback.run();
        }
    }

    private IllegalStateException remoteFailure(Exception ex) {
        if (ex instanceof IllegalStateException illegalStateException) {
            return illegalStateException;
        }
        String message = ex.getMessage();
        return new IllegalStateException(message == null || message.isBlank() ? "远程接口请求失败" : message, ex);
    }

    private static boolean strictRemoteMode() {
        return Boolean.getBoolean("tihu.remote.strict")
                || "true".equalsIgnoreCase(System.getenv("TIHU_REMOTE_STRICT"));
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class RemoteApiException extends IllegalStateException {
        private RemoteApiException(String message) {
            super(message);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatRemoteErrorMessage(String message, JsonNode data) {
        String text = message == null || message.isBlank() ? "远程接口请求失败" : message;
        if (text.contains("封禁")) {
            String until = text(firstPresent(data, "bannedUntil", "banExpireTime", "until", "unbanTime"));
            if (until != null && !until.isBlank()) {
                return "您已被封禁，解封时间是 " + until;
            }
            if (!text.contains("您已被封禁")) {
                return "您已被封禁" + (text.contains("解封时间") ? "" : "，请联系管理员确认解封时间");
            }
        }
        return text;
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private Long resolveUserId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        if (Objects.equals(username, currentUsername) && currentUserId != null) {
            return currentUserId;
        }
        if (userIdCache.containsKey(username)) {
            return userIdCache.get(username);
        }
        String[] candidates = {
                "/users/profile/" + username,
                "/users/by-username/" + username,
                "/users/" + username
        };
        for (String path : candidates) {
            try {
                JsonNode data = requestData("GET", path, null);
                Long id = longNullable(firstPresent(data, "id", "userId"));
                if (id == null || id <= 0) {
                    JsonNode userInfo = firstPresent(data, "userInfo", "user", "data");
                    id = longNullable(firstPresent(userInfo, "id", "userId"));
                }
                if (id != null && id > 0) {
                    userIdCache.put(username, id);
                    return id;
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private String resolveFollowQuery(String username) {
        Long id = resolveUserId(username);
        if (id != null) {
            return query(Map.of("followeeId", id));
        }
        return query(Map.of("followeeUsername", username));
    }

    private String resolveFollowListPath(String username, boolean followers) {
        String basePath = followers ? "/follows/followers" : "/follows/followees";
        if (username == null || username.isBlank() || Objects.equals(username, currentUsername)) {
            return basePath + query(Map.of("page", 1, "size", 1000));
        }
        Long id = resolveUserId(username);
        if (id == null) {
            throw new IllegalStateException("无法解析用户ID：" + username);
        }
        return "/follows/user/" + id + (followers ? "/followers" : "/followees") + query(Map.of("page", 1, "size", 1000));
    }

    private String resolveMessageQuery(String peer, String content) {
        Long id = resolveUserId(peer);
        Map<String, Object> params = new LinkedHashMap<>();
        if (id != null) {
            params.put("receiverId", id);
        } else {
            params.put("receiverUsername", peer);
        }
        params.put("content", content);
        return query(params);
    }

    private String resolveConversationPeer(String peer) {
        Long id = resolveUserId(peer);
        return id == null ? peer : String.valueOf(id);
    }

    private CommentItem parseCommentItem(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        long id = longValue(firstPresent(node, "id", "commentId"), 0L);
        if (id <= 0) {
            return null;
        }
        String user = safe(text(firstPresent(node, "user", "username", "nickname")));
        String content = safe(text(firstPresent(node, "content", "text")));
        LocalDateTime time = parseDateTime(text(firstPresent(node, "time", "createTime", "createdAt")));
        Long parentId = longNullable(firstPresent(node, "parentId", "parentCommentId", "replyTo"));
        int upVotes = intValue(firstPresent(node, "upVotes", "upvoteCount", "likes", "likeCount"));
        int downVotes = intValue(firstPresent(node, "downVotes", "downvoteCount", "dislikes", "dislikeCount"));
        JsonNode bookNode = firstPresent(node, "book", "bookInfo");
        Long bookId = longNullable(firstPresent(node, "bookId", "book_id"));
        if (bookId == null && bookNode != null) {
            bookId = longNullable(firstPresent(bookNode, "id", "bookId", "book_id"));
        }
        String bookTitle = safe(text(firstPresent(node, "bookTitle", "title")));
        if (bookTitle.isBlank() && bookNode != null) {
            bookTitle = safe(text(firstPresent(bookNode, "title", "bookTitle")));
        }
        return new CommentItem(id, user, content, time, parentId, upVotes, downVotes, bookId, bookTitle);
    }

    private UserBookList parseBookList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        long id = longValue(firstPresent(node, "id", "listId"), 0L);
        if (id <= 0) {
            return null;
        }
        String owner = safe(text(firstPresent(node, "owner", "username", "user")));
        String title = safe(text(firstPresent(node, "title")));
        String intro = safe(text(firstPresent(node, "description", "intro")));
        List<Long> bookIds = new ArrayList<>();
        JsonNode ids = firstPresent(node, "bookIds", "bookIdList", "books");
        if (ids != null && ids.isArray()) {
            for (JsonNode item : ids) {
                long bookId = longValue(item, 0L);
                if (bookId > 0) {
                    bookIds.add(bookId);
                }
            }
        }
        boolean publicVisible = parsePublicVisible(node);
        return new UserBookList(id, owner, title, intro, bookIds, publicVisible);
    }

    private boolean parsePublicVisible(JsonNode node) {
        JsonNode publicNode = firstPresent(node, "publicVisible", "isPublic", "public", "visible");
        if (publicNode != null && !publicNode.isMissingNode() && !publicNode.isNull()) {
            if (publicNode.isBoolean()) {
                return publicNode.asBoolean();
            }
            String value = publicNode.asText();
            return "true".equalsIgnoreCase(value) || "1".equals(value) || "PUBLIC".equalsIgnoreCase(value)
                    || "公开".equals(value);
        }
        String visibility = text(firstPresent(node, "visibility", "privacy", "mode"));
        if (visibility == null || visibility.isBlank()) {
            return true;
        }
        return !"PRIVATE".equalsIgnoreCase(visibility) && !"私密".equals(visibility);
    }

    private FollowItem parseFollowItem(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String username = extractUsername(node);
        return username.isBlank() ? null : new FollowItem(username);
    }

    private String extractUsername(JsonNode node) {
        String username = safe(text(firstPresent(node, "username", "user", "followeeUsername", "followerUsername",
                "nickname", "name")));
        if (!username.isBlank()) {
            return username;
        }
        JsonNode nested = firstPresent(node, "userInfo", "userInfoVO", "user", "followee", "follower", "profile");
        if (nested != null && nested != node) {
            return extractUsername(nested);
        }
        return "";
    }

    private ConversationPreview parseConversationPreview(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String peer = safe(text(firstPresent(node, "peer", "username", "user", "peerUsername", "targetUsername",
                "otherUsername")));
        if (peer.isBlank()) {
            JsonNode peerNode = firstPresent(node, "peerUser", "targetUser", "otherUser", "userInfo", "user");
            if (peerNode != null && peerNode != node) {
                peer = extractUsername(peerNode);
            }
        }
        if (peer.isBlank()) {
            return null;
        }
        String lastMessage = safe(text(firstPresent(node, "lastMessage", "lastContent", "content", "message")));
        LocalDateTime time = parseDateTime(text(firstPresent(node, "lastTime", "time", "updatedAt", "createTime",
                "createdAt")));
        return new ConversationPreview(peer, lastMessage, time);
    }

    private MessageItem parseMessageItem(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String from = safe(text(firstPresent(node, "from", "sender", "senderUsername", "fromUsername",
                "senderName")));
        if (from.isBlank()) {
            JsonNode senderNode = firstPresent(node, "senderUser", "senderInfo", "sender");
            if (senderNode != null && senderNode != node) {
                from = extractUsername(senderNode);
            }
        }
        String to = safe(text(firstPresent(node, "to", "receiver", "receiverUsername", "toUsername",
                "receiverName")));
        if (to.isBlank()) {
            JsonNode receiverNode = firstPresent(node, "receiverUser", "receiverInfo", "receiver");
            if (receiverNode != null && receiverNode != node) {
                to = extractUsername(receiverNode);
            }
        }
        String content = safe(text(firstPresent(node, "content", "message", "text")));
        LocalDateTime time = parseDateTime(text(firstPresent(node, "time", "createTime", "createdAt")));
        if (from.isBlank() || to.isBlank()) {
            return null;
        }
        return new MessageItem(from, to, content, time);
    }

    private BanInfo parseBanInfo(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String username = safe(text(firstPresent(node, "username", "user")));
        LocalDateTime until = parseDateTime(text(firstPresent(node, "bannedUntil", "banExpireTime", "until", "unbanTime")));
        if (username.isBlank()) {
            return null;
        }
        return new BanInfo(username, until);
    }

    private AdminUserInfo parseAdminUserInfo(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode userNode = firstPresent(node, "userInfo", "user", "profile", "account");
        if (userNode == null || userNode.isMissingNode() || userNode.isNull()) {
            userNode = node;
        }
        String username = safe(text(firstPresent(userNode, "username", "user", "name")));
        if (username.isBlank()) {
            return null;
        }
        Role role = parseRole(text(firstPresent(userNode, "role", "userRole")));
        if (role == null) {
            role = parseRole(text(firstPresent(node, "role", "userRole")));
        }
        if (role == null) {
            role = Role.USER;
        }
        LocalDateTime createdAt = parseNullableDateTime(text(firstPresent(userNode, "createdAt", "createTime",
                "registerTime", "registeredAt", "registrationTime")));
        if (createdAt == null && userNode != node) {
            createdAt = parseNullableDateTime(text(firstPresent(node, "createdAt", "createTime", "registerTime",
                    "registeredAt", "registrationTime")));
        }
        LocalDateTime bannedUntil = parseNullableDateTime(text(firstPresent(userNode, "bannedUntil", "banExpireTime",
                "until", "unbanTime")));
        if (bannedUntil == null && userNode != node) {
            bannedUntil = parseNullableDateTime(text(firstPresent(node, "bannedUntil", "banExpireTime", "until",
                    "unbanTime")));
        }
        if (bannedUntil != null && !bannedUntil.isAfter(LocalDateTime.now())) {
            bannedUntil = null;
        }
        return new AdminUserInfo(username, role, createdAt, bannedUntil);
    }

    private UserProfile parseUserProfile(JsonNode node, String target) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode userInfo = firstPresent(node, "userInfo", "user", "profile");
        if (userInfo == null || userInfo.isMissingNode() || userInfo.isNull()) {
            userInfo = node;
        }
        String username = safe(text(firstPresent(userInfo, "username", "user", "name")));
        if (username.isBlank()) {
            username = target;
        }
        List<CommentItem> comments = parseComments(firstPresent(node, "comments", "commentList", "records"));
        List<UserBookList> lists = new ArrayList<>();
        JsonNode listNode = firstPresent(node, "bookLists", "lists", "records");
        if (listNode != null && listNode.isArray()) {
            for (JsonNode item : listNode) {
                UserBookList list = parseBookList(item);
                if (list != null) {
                    if (list.owner().isBlank()) {
                        list = new UserBookList(list.id(), username, list.title(), list.intro(), list.bookIds(),
                                list.publicVisible());
                    }
                    lists.add(list);
                }
            }
        }
        int followingCount = intValue(firstPresent(node, "followingCount", "following"));
        int followerCount = intValue(firstPresent(node, "followerCount", "followers"));
        boolean followed = false;
        JsonNode followedNode = firstPresent(node, "followedByCurrentUser", "followed");
        if (followedNode != null) {
            followed = followedNode.isBoolean() ? followedNode.asBoolean() : Boolean.parseBoolean(followedNode.asText());
        }
        String avatarImage = safe(text(firstPresent(userInfo, "avatarImage", "avatar", "avatarUrl", "profileImage", "profileImageUrl")));
        return new UserProfile(username, comments, lists, followingCount, followerCount, followed,
                avatarImage.isBlank() ? null : avatarImage);
    }

    private boolean containsKeyword(String text, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private long longValue(JsonNode node, long defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        return node.asLong(defaultValue);
    }

    private int intValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        return node.asInt();
    }

    private Integer intNullable(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asInt();
    }

    private Long longNullable(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asLong();
    }

    private double doubleValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0D;
        }
        return node.asDouble();
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            return LocalDateTime.now();
        }
    }

    private LocalDateTime parseNullableDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDateTime(value);
    }

    private JsonNode firstPresent(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            JsonNode child = node.get(name);
            if (child != null && !child.isMissingNode() && !child.isNull()) {
                return child;
            }
        }
        return null;
    }

    private List<String> parseStringList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            String value = text(node);
            return value == null || value.isBlank() ? List.of() : List.of(value);
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            String value = text(item);
            if (value != null && !value.isBlank()) {
                result.add(value.trim());
            }
        }
        return result;
    }

    private String formatAvg(RatingSummary summary) {
        if (summary == null || summary.count() <= 0) {
            return "暂无评分";
        }
        return String.format(Locale.US, "%.1f", summary.average());
    }

    private Map<Integer, Integer> defaultDistribution() {
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int i = 1; i <= 10; i++) {
            distribution.put(i, 0);
        }
        return distribution;
    }

    private String query(Map<String, ?> params) {
        return ApiClient.encodeQuery(params);
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
