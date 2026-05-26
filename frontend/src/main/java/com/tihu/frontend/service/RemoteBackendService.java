package com.tihu.frontend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tihu.frontend.request.ApiClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
        return remoteOrFallback(() -> remoteLogin(username, password), () -> super.login(username, password));
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
        runRemoteOrFallback(() -> remoteUpdateProfile(currentUsername, newUsername, newPassword),
                () -> super.updateProfile(currentUsername, newUsername, newPassword));
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
                    book = new Book(book.id(), book.title(), book.author(), book.intro(), tags);
                }
            }
            RatingSummary summary = parseRatingSummary(firstPresent(data, "ratings", "ratingSummary", "data"), bookId, currentUser);
            if (summary == null) {
                summary = fetchRatingSummary(bookId, currentUser);
            }
            List<CommentItem> comments = parseComments(firstPresent(data, "comments", "commentList", "data"));
            List<CommentItem> replies = parseComments(firstPresent(data, "replies", "replyList"));
            if (comments.isEmpty() && replies.isEmpty()) {
                comments = parseComments(requestPageData("/comments/book/" + bookId + query(Map.of("page", 1, "size", 100))));
            }
            return new BookDetail(book, summary, comments, replies);
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
    public synchronized BookListPage listBooks(String titleKeyword, List<String> tags, int page, int pageSize) {
        return remoteOrFallback(() -> {
            List<BookCard> catalog = fetchCatalog(titleKeyword, tags);
            int safePage = Math.max(1, page);
            int safePageSize = Math.max(1, pageSize);
            int totalItems = catalog.size();
            int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safePageSize));
            int from = Math.min((safePage - 1) * safePageSize, totalItems);
            int to = Math.min(from + safePageSize, totalItems);
            return new BookListPage(new ArrayList<>(catalog.subList(from, to)), safePage, totalPages, totalItems);
        }, () -> super.listBooks(titleKeyword, tags, page, pageSize));
    }

    @Override
    public synchronized void updateBook(long bookId, String title, String author, String intro, String tagsText) {
        runRemoteOrFallback(() -> {
            List<String> tags = parseTagInput(tagsText);
            requestData("PUT", "/books/" + bookId, json(mapOf(
                    "title", title,
                    "author", author,
                    "description", intro,
                    "cover", "",
                    "tags", tags
            )));
        }, () -> super.updateBook(bookId, title, author, intro, tagsText));
    }

    @Override
    public synchronized void updateBook(int bookId, String title, String author, String intro, String tagsText) {
        updateBook((long) bookId, title, author, intro, tagsText);
    }

    @Override
    public synchronized Book addBook(String title, String author, String intro, String tagsText) {
        return remoteOrFallback(() -> {
            List<String> tags = parseTagInput(tagsText);
            JsonNode data = requestData("POST", "/books", json(mapOf(
                    "title", title,
                    "author", author,
                    "description", intro,
                    "cover", "",
                    "tags", tags
            )));
            Book book = parseBook(firstPresent(data, "book", "data", "bookInfo"), -1L);
            if (book != null) {
                return book;
            }
            return super.addBook(title, author, intro, tagsText);
        }, () -> super.addBook(title, author, intro, tagsText));
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
            return item != null ? item : super.addComment(bookId, username, content, parentCommentId);
        }, () -> super.addComment(bookId, username, content, parentCommentId));
    }

    @Override
    public synchronized CommentItem addComment(int bookId, String username, String content, Long parentCommentId) {
        return addComment((long) bookId, username, content, parentCommentId);
    }

    @Override
    public synchronized void deleteOwnComment(long bookId, long commentId, String username) {
        runRemoteOrFallback(() -> requestData("DELETE", "/comments/" + commentId, null),
                () -> super.deleteOwnComment(bookId, commentId, username));
    }

    @Override
    public synchronized void deleteOwnComment(int bookId, long commentId, String username) {
        deleteOwnComment((long) bookId, commentId, username);
    }

    @Override
    public synchronized void adminDeleteComment(long bookId, long commentId) {
        runRemoteOrFallback(() -> requestData("DELETE", "/comments/admin/" + commentId, null),
                () -> super.adminDeleteComment(bookId, commentId));
    }

    @Override
    public synchronized void adminDeleteComment(int bookId, long commentId) {
        adminDeleteComment((long) bookId, commentId);
    }

    @Override
    public synchronized void adminDeleteComment(long commentId) {
        runRemoteOrFallback(() -> requestData("DELETE", "/comments/admin/" + commentId, null),
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
        return remoteOrFallback(() -> {
            JsonNode data = requestData("POST", "/book-lists" + query(mapOf("title", title, "description", intro)), null);
            UserBookList list = parseBookList(firstPresent(data, "bookList", "data", "list"));
            return list != null ? list : super.createBookList(username, title, intro);
        }, () -> super.createBookList(username, title, intro));
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
            JsonNode page = requestPageData("/follows/followees" + query(Map.of("page", 1, "size", 1000)));
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
            JsonNode page = requestPageData("/follows/followers" + query(Map.of("page", 1, "size", 1000)));
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
            JsonNode data = requestData("GET", "/users/profile" + query(Map.of("username", target)), null);
            UserProfile profile = parseUserProfile(data, target, currentUser);
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
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/collections" + query(Map.of("page", 1, "size", 1000)));
            List<BookCard> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                BookCard card = parseFavoriteCard(item, username);
                if (card != null) {
                    result.add(card);
                }
            }
            return result;
        }, () -> super.listFavorites(username));
    }

    private List<BookCard> fetchCatalog(String titleKeyword, List<String> tags) throws IOException, InterruptedException {
        boolean needTags = tags != null && !tags.isEmpty();
        JsonNode page = needTags
                ? requestPageData("/books/search-by-tags" + query(mapOf("tags", tags, "page", 1, "size", 1000)))
                : requestPageData("/books" + query(Map.of("page", 1, "size", 1000)));
        List<BookCard> result = new ArrayList<>();
        for (JsonNode item : pageRecords(page)) {
            BookCard card = parseBookCard(item, currentUsername);
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
                        return lowerNeed.stream().allMatch(tag -> cardTags.stream()
                                .map(v -> v.toLowerCase(Locale.ROOT))
                                .collect(Collectors.toSet())
                                .contains(tag));
                    })
                    .toList();
        }
        return result;
    }

    private BookCard parseBookCard(JsonNode item, String currentUser) throws IOException, InterruptedException {
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
        RatingSummary summary = parseRatingSummary(item, bookId, currentUser);
        JsonNode nestedRatings = firstPresent(item, "ratings", "ratingSummary");
        if (nestedRatings != null) {
            RatingSummary nestedSummary = parseRatingSummary(nestedRatings, bookId, currentUser);
            if (nestedSummary != null && (summary == null || nestedSummary.count() > 0)) {
                summary = nestedSummary;
            }
        }
        if (summary == null || summary.count() == 0) {
            summary = fetchRatingSummary(bookId, currentUser);
        }
        if (title == null) {
            title = "";
        }
        if (author == null) {
            author = "";
        }
        return new BookCard(bookId, title, author, String.join(" ", tags), formatAvg(summary));
    }

    private BookCard parseFavoriteCard(JsonNode item, String username) throws IOException, InterruptedException {
        if (item == null || item.isMissingNode() || item.isNull()) {
            return null;
        }
        String owner = safe(text(firstPresent(item, "owner", "username", "user", "nickname")));
        if (owner != null && !owner.isBlank() && username != null && !username.isBlank() && !owner.equals(username)) {
            return null;
        }
        BookCard card = parseBookCard(item, username);
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

        RatingSummary summary = parseRatingSummary(item, bookId, username);
        if (summary == null || summary.count() == 0) {
            summary = fetchRatingSummary(bookId, username);
        }
        return new BookCard(bookId, title, author, String.join(" ", tags), formatAvg(summary));
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
        return new Book(id, title, author, intro, tags);
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
        return List.of(value.trim().split("\\s+"))
                .stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private List<String> parseTagInput(String tagsText) {
        if (tagsText == null || tagsText.isBlank()) {
            return List.of();
        }
        return List.of(tagsText.trim().split("\\s+"))
                .stream()
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

    private boolean isCollected(int bookId) {
        return isCollected((long) bookId);
    }

    private RatingSummary fetchRatingSummary(long bookId, String currentUser) {
        try {
            JsonNode data = requestData("GET", "/ratings/book/" + bookId + "/stats", null);
            return parseRatingSummary(data, bookId, currentUser);
        } catch (Exception ex) {
            return null;
        }
    }

    private RatingSummary parseRatingSummary(JsonNode node, long bookId, String currentUser) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        double avg = doubleValue(firstPresent(node, "average", "avg", "avgScore", "averageScore"), 0D);
        int count = intValue(firstPresent(node, "count", "ratingCount", "total"), 0);
        Map<Integer, Integer> distribution = defaultDistribution();
        JsonNode distNode = firstPresent(node, "distribution", "ratingDistribution");
        if (distNode != null && distNode.isObject()) {
            for (int i = 1; i <= 10; i++) {
                distribution.put(i, intValue(distNode.get(String.valueOf(i)), 0));
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
                long id = longValue(firstPresent(item, "id", "commentId"), 0L);
                if (id <= 0) {
                    continue;
                }
                String content = safe(text(firstPresent(item, "content", "text")));
                String user = safe(text(firstPresent(item, "username", "user", "nickname")));
                LocalDateTime time = parseDateTime(text(firstPresent(item, "createTime", "time", "createdAt")));
                Long parentId = longNullable(firstPresent(item, "parentId", "replyTo"));
                result.add(new CommentItem(id, user, content, time, parentId, 0, 0));
            }
        }
        return result;
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
            throw new IllegalStateException(message == null || message.isBlank() ? "远程接口请求失败" : message);
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
        if (currentUsername != null && currentUserId != null) {
            // remote user id may be used later by id-based endpoints
        }
        if (role == null) {
            role = parseRole(text(firstPresent(data, "role")));
        }
        if (role == null) {
            throw new IllegalStateException("登录响应缺少角色信息");
        }
        return role;
    }

    private void remoteUpdateProfile(String currentUsername, String newUsername, String newPassword) throws IOException, InterruptedException {
        JsonNode payload = requestData("PUT", "/users/profile", json(mapOf(
                "currentUsername", currentUsername,
                "newUsername", newUsername,
                "newPassword", newPassword
        )));
        if (payload == null) {
            requestData("PUT", "/users/me", json(mapOf(
                    "username", currentUsername,
                    "newUsername", newUsername,
                    "newPassword", newPassword
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
            return fallback.get();
        }
    }

    private void runRemoteOrFallback(ThrowingRunnable remote, Runnable fallback) {
        try {
            remote.run();
        } catch (Exception ex) {
            fallback.run();
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
                "/users/profile" + query(Map.of("username", username)),
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
        Long parentId = longNullable(firstPresent(node, "parentId", "replyTo"));
        int upVotes = intValue(firstPresent(node, "upVotes", "upvoteCount", "likes"), 0);
        int downVotes = intValue(firstPresent(node, "downVotes", "downvoteCount", "dislikes"), 0);
        return new CommentItem(id, user, content, time, parentId, upVotes, downVotes);
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
        return new UserBookList(id, owner, title, intro, bookIds);
    }

    private FollowItem parseFollowItem(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String username = safe(text(firstPresent(node, "username", "user", "followeeUsername", "nickname")));
        return username.isBlank() ? null : new FollowItem(username);
    }

    private ConversationPreview parseConversationPreview(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String peer = safe(text(firstPresent(node, "peer", "username", "user", "peerUsername")));
        if (peer.isBlank()) {
            return null;
        }
        String lastMessage = safe(text(firstPresent(node, "lastMessage", "content", "message")));
        LocalDateTime time = parseDateTime(text(firstPresent(node, "lastTime", "time", "updatedAt")));
        return new ConversationPreview(peer, lastMessage, time);
    }

    private MessageItem parseMessageItem(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String from = safe(text(firstPresent(node, "from", "sender", "senderUsername")));
        String to = safe(text(firstPresent(node, "to", "receiver", "receiverUsername")));
        String content = safe(text(firstPresent(node, "content", "message")));
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
        LocalDateTime until = parseDateTime(text(firstPresent(node, "bannedUntil", "until")));
        if (username.isBlank()) {
            return null;
        }
        return new BanInfo(username, until);
    }

    private UserProfile parseUserProfile(JsonNode node, String target, String currentUser) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String username = safe(text(firstPresent(node, "username", "user", "name")));
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
                    lists.add(list);
                }
            }
        }
        int followingCount = intValue(firstPresent(node, "followingCount", "following"), 0);
        int followerCount = intValue(firstPresent(node, "followerCount", "followers"), 0);
        boolean followed = false;
        JsonNode followedNode = firstPresent(node, "followedByCurrentUser", "followed");
        if (followedNode != null) {
            followed = followedNode.isBoolean() ? followedNode.asBoolean() : Boolean.parseBoolean(followedNode.asText());
        }
        return new UserProfile(username, comments, lists, followingCount, followerCount, followed);
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

    private int intValue(JsonNode node, int defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        return node.asInt(defaultValue);
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

    private double doubleValue(JsonNode node, double defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        return node.asDouble(defaultValue);
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
        String q = ApiClient.encodeQuery(params);
        return q == null ? "" : q;
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
