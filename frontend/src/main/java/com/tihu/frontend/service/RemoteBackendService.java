package com.tihu.frontend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tihu.frontend.request.ApiClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private String currentPassword;
    private Role currentRole;

    public RemoteBackendService() {
        this(new ApiClient());
    }

    public RemoteBackendService(ApiClient apiClient) {
        super();
        this.apiClient = apiClient;
    }

    @Override
    public void logout() {
        apiClient.setToken(null);
        apiClient.setSessionCookie(null);
        currentUserId = null;
        currentUsername = null;
        currentPassword = null;
        currentRole = null;
    }

    @Override
    public String token() {
        return apiClient.token();
    }

    @Override
    public synchronized Role login(String username, String password) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("POST", "/users/login", json(Map.of("username", username, "password", password)));
            JsonNode userInfo = firstPresent(data, "userInfo", "user", "data");
            if (userInfo == null || userInfo.isMissingNode() || userInfo.isNull()) {
                throw new RemoteApiException("登录响应缺少 userInfo");
            }

            String token = text(firstPresent(data, "token", "accessToken"));
            if (token != null && !token.isBlank()) {
                apiClient.setToken(token);
            }

            currentUserId = longValue(userInfo, "id", null);
            currentUsername = text(firstPresent(userInfo, "username"));
            currentPassword = password;
            currentRole = parseRole(text(firstPresent(userInfo, "role")));
            if (currentRole == null) {
                currentRole = Role.USER;
            }
            return currentRole;
        }, () -> {
            Role role = super.login(username, password);
            currentUsername = username;
            currentPassword = password;
            currentRole = role;
            return role;
        });
    }

    @Override
    public synchronized void registerUser(String username, String password) {
        remoteOrFallback(() -> {
            requestData("POST", "/users/register", json(Map.of("username", username, "password", password)));
            currentUsername = username;
            currentPassword = password;
            currentRole = Role.USER;
            return null;
        }, () -> {
            super.registerUser(username, password);
            return null;
        });
    }

    @Override
    public synchronized void registerAdmin(String username, String password, String inviteCode) {
        remoteOrFallback(() -> {
            requestData("POST", "/users/register" + query(Map.of("inviteCode", inviteCode)), json(Map.of("username", username, "password", password)));
            currentUsername = username;
            currentPassword = password;
            currentRole = Role.ADMIN;
            return null;
        }, () -> {
            super.registerAdmin(username, password, inviteCode);
            return null;
        });
    }

    @Override
    public synchronized void updateProfile(String currentUsername, String newUsername, String newPassword) {
        remoteOrFallback(() -> {
            Long userId = requireCurrentUserId();
            if (newUsername != null && !newUsername.isBlank() && !Objects.equals(currentUsername, newUsername.trim())) {
                requestData("PUT", "/users/" + userId + "/username" + query(Map.of("newUsername", newUsername.trim())), null);
                this.currentUsername = newUsername.trim();
            }
            if (newPassword != null && !newPassword.isBlank()) {
                if (currentPassword == null || currentPassword.isBlank()) {
                    throw new RemoteApiException("当前会话缺少旧密码，请重新登录后再修改密码");
                }
                requestData("PUT", "/users/" + userId + "/password" + query(Map.of("oldPassword", currentPassword, "newPassword", newPassword)), null);
                currentPassword = newPassword;
            }
            mirror(() -> super.updateProfile(currentUsername, newUsername, newPassword));
            return null;
        }, () -> {
            super.updateProfile(currentUsername, newUsername, newPassword);
            if (newUsername != null && !newUsername.isBlank()) {
                this.currentUsername = newUsername.trim();
            }
            if (newPassword != null && !newPassword.isBlank()) {
                currentPassword = newPassword;
            }
            return null;
        });
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
    public synchronized BookDetail getBookDetail(int bookId, String currentUser) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/books/" + bookId, null);
            Book book = parseBook(firstPresent(data, "bookInfo", "book", "data"), bookId);
            RatingSummary summary = parseRatingSummary(firstPresent(data, "ratings", "ratingSummary", "data"), bookId, currentUser);
            if (summary == null) {
                summary = fetchRatingSummary(bookId, currentUser);
            }

            List<CommentItem> comments = parseComments(firstPresent(data, "comments"));
            List<CommentItem> replies = parseComments(firstPresent(data, "replies"));
            if (comments.isEmpty() && replies.isEmpty()) {
                comments = parseComments(requestPageData("/comments/book/" + bookId + query(Map.of("page", 1, "size", 100))));
            }

            return new BookDetail(book, summary, comments, replies);
        }, () -> super.getBookDetail(bookId, currentUser));
    }

    @Override
    public synchronized void rateBook(int bookId, String username, int score) {
        remoteOrFallback(() -> {
            requestData("POST", "/ratings" + query(Map.of("bookId", bookId, "score", score)), null);
            mirror(() -> super.rateBook(bookId, username, score));
            return null;
        }, () -> {
            super.rateBook(bookId, username, score);
            return null;
        });
    }

    @Override
    public synchronized void toggleFavorite(String username, int bookId) {
        remoteOrFallback(() -> {
            boolean collected = isCollected(bookId);
            if (collected) {
                requestData("DELETE", "/collections" + query(Map.of("bookId", bookId)), null);
            } else {
                requestData("POST", "/collections" + query(Map.of("bookId", bookId)), null);
            }
            mirror(() -> super.toggleFavorite(username, bookId));
            return null;
        }, () -> {
            super.toggleFavorite(username, bookId);
            return null;
        });
    }

    @Override
    public synchronized List<BookCard> listFavorites(String username) {
        return remoteOrFallback(() -> {
            JsonNode data = requestPageData("/collections" + query(Map.of("page", 1, "size", 100)));
            List<BookCard> result = new ArrayList<>();
            for (JsonNode item : pageRecords(data)) {
                int bookId = intValue(firstPresent(item, "bookId", "id"), -1);
                if (bookId <= 0) {
                    continue;
                }
                BookDetail detail = getBookDetail(bookId, username);
                result.add(new BookCard(detail.book().id(), detail.book().title(), detail.book().author(),
                        String.join(", ", detail.book().tags()), formatAvg(detail.ratingSummary())));
            }
            return result;
        }, () -> super.listFavorites(username));
    }

    @Override
    public synchronized CommentItem addComment(int bookId, String username, String content, Long parentCommentId) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("POST", "/comments" + query(mapOf(
                    "bookId", bookId,
                    "content", content,
                    "parentCommentId", parentCommentId
            )), null);
            CommentItem item = parseComment(firstPresent(data, "comment", "data"), username);
            if (item == null) {
                item = new CommentItem(longValue(firstPresent(data, "id"), System.currentTimeMillis()), username,
                        content.trim(), LocalDateTime.now(), parentCommentId, 0, 0);
            }
            mirror(() -> super.addComment(bookId, username, content, parentCommentId));
            return item;
        }, () -> super.addComment(bookId, username, content, parentCommentId));
    }

    @Override
    public synchronized void updateBook(int bookId, String title, String author, String intro, String tagsText) {
        remoteOrFallback(() -> {
            requestData("PUT", "/books/" + bookId, json(mapOf(
                    "title", title,
                    "author", safe(author),
                    "description", safe(intro),
                    "cover", ""
            )));
            mirror(() -> super.updateBook(bookId, title, author, intro, tagsText));
            return null;
        }, () -> {
            super.updateBook(bookId, title, author, intro, tagsText);
            return null;
        });
    }

    @Override
    public synchronized void deleteOwnComment(int bookId, long commentId, String username) {
        remoteOrFallback(() -> {
            requestData("DELETE", "/comments/" + commentId, null);
            mirror(() -> super.deleteOwnComment(bookId, commentId, username));
            return null;
        }, () -> {
            super.deleteOwnComment(bookId, commentId, username);
            return null;
        });
    }

    @Override
    public synchronized void adminDeleteComment(int bookId, long commentId) {
        remoteOrFallback(() -> {
            requestData("DELETE", "/comments/admin/" + commentId, null);
            mirror(() -> super.adminDeleteComment(bookId, commentId));
            return null;
        }, () -> {
            super.adminDeleteComment(bookId, commentId);
            return null;
        });
    }

    @Override
    public synchronized void adminDeleteComment(long commentId) {
        remoteOrFallback(() -> {
            requestData("DELETE", "/comments/admin/" + commentId, null);
            mirror(() -> super.adminDeleteComment(commentId));
            return null;
        }, () -> {
            super.adminDeleteComment(commentId);
            return null;
        });
    }

    @Override
    public synchronized void voteComment(long commentId, String username, int target) {
        remoteOrFallback(() -> {
            if (target == 1) {
                requestData("POST", "/comments/" + commentId + "/like", null);
            } else if (target == -1) {
                requestData("POST", "/comments/" + commentId + "/dislike", null);
            } else {
                requestData("DELETE", "/comments/" + commentId + "/like", null);
            }
            mirror(() -> super.voteComment(commentId, username, target));
            return null;
        }, () -> {
            super.voteComment(commentId, username, target);
            return null;
        });
    }

    @Override
    public synchronized List<UserBookList> listBookLists(String username) {
        return remoteOrFallback(() -> {
            Long userId = resolveUserId(username);
            JsonNode page = requestPageData("/book-lists" + query(Map.of("page", 1, "size", 100)));
            List<UserBookList> lists = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                if (intValue(firstPresent(item, "userId", "ownerId"), -1) != userId.intValue()) {
                    continue;
                }
                long listId = longValue(item, "id", null);
                UserBookList detail = getBookList(username, listId);
                lists.add(detail);
            }
            return lists;
        }, () -> super.listBookLists(username));
    }

    @Override
    public synchronized UserBookList createBookList(String username, String title, String intro) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("POST", "/book-lists" + query(mapOf("title", title, "description", intro)), null);
            UserBookList list = parseBookListDetail(firstPresent(data, "data", "bookList", "book_list"), username);
            if (list == null) {
                long id = longValue(firstPresent(data, "id"), System.currentTimeMillis());
                list = new UserBookList(id, username, title == null ? "" : title.trim(), intro == null ? "" : intro.trim(), new ArrayList<>());
            }
            mirror(() -> super.createBookList(username, title, intro));
            return list;
        }, () -> super.createBookList(username, title, intro));
    }

    @Override
    public synchronized void deleteBookList(String username, long listId) {
        remoteOrFallback(() -> {
            requestData("DELETE", "/book-lists/" + listId, null);
            mirror(() -> super.deleteBookList(username, listId));
            return null;
        }, () -> {
            super.deleteBookList(username, listId);
            return null;
        });
    }

    @Override
    public synchronized UserBookList getBookList(String owner, long listId) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/book-lists/" + listId, null);
            return parseBookListDetail(data, owner);
        }, () -> super.getBookList(owner, listId));
    }

    @Override
    public synchronized void addBookToBookList(String username, long listId, int bookId) {
        remoteOrFallback(() -> {
            requestData("POST", "/book-lists/" + listId + "/books" + query(Map.of("bookId", bookId)), null);
            mirror(() -> super.addBookToBookList(username, listId, bookId));
            return null;
        }, () -> {
            super.addBookToBookList(username, listId, bookId);
            return null;
        });
    }

    @Override
    public synchronized void removeBookFromBookList(String username, long listId, int bookId) {
        remoteOrFallback(() -> {
            requestData("DELETE", "/book-lists/" + listId + "/books" + query(Map.of("bookId", bookId)), null);
            mirror(() -> super.removeBookFromBookList(username, listId, bookId));
            return null;
        }, () -> {
            super.removeBookFromBookList(username, listId, bookId);
            return null;
        });
    }

    @Override
    public synchronized void follow(String me, String target) {
        remoteOrFallback(() -> {
            long targetId = resolveUserId(target);
            requestData("POST", "/follows" + query(Map.of("followeeId", targetId)), null);
            mirror(() -> super.follow(me, target));
            return null;
        }, () -> {
            super.follow(me, target);
            return null;
        });
    }

    @Override
    public synchronized void unfollow(String me, String target) {
        remoteOrFallback(() -> {
            long targetId = resolveUserId(target);
            requestData("DELETE", "/follows" + query(Map.of("followeeId", targetId)), null);
            mirror(() -> super.unfollow(me, target));
            return null;
        }, () -> {
            super.unfollow(me, target);
            return null;
        });
    }

    @Override
    public synchronized List<FollowItem> listFollowing(String username) {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/follows/followees" + query(Map.of("page", 1, "size", 100)));
            List<FollowItem> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                Long followeeId = longValue(firstPresent(item, "followeeId", "userId", "targetId"), null);
                if (followeeId != null) {
                    result.add(new FollowItem(resolveUsername(followeeId)));
                }
            }
            return result;
        }, () -> super.listFollowing(username));
    }

    @Override
    public synchronized List<FollowItem> listFollowers(String username) {
        return remoteOrFallback(() -> {
            JsonNode page = requestPageData("/follows/followers" + query(Map.of("page", 1, "size", 100)));
            List<FollowItem> result = new ArrayList<>();
            for (JsonNode item : pageRecords(page)) {
                Long followerId = longValue(firstPresent(item, "followerId", "userId", "sourceId"), null);
                if (followerId != null) {
                    result.add(new FollowItem(resolveUsername(followerId)));
                }
            }
            return result;
        }, () -> super.listFollowers(username));
    }

    @Override
    public synchronized UserProfile getUserProfile(String target, String currentUser) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/users/profile/" + encodePathSegment(target), null);
            JsonNode userInfo = firstPresent(data, "userInfo", "user", "data");
            if (userInfo == null || userInfo.isMissingNode() || userInfo.isNull()) {
                throw new RemoteApiException("用户主页响应缺少 userInfo");
            }

            String username = text(firstPresent(userInfo, "username"));
            if (username == null || username.isBlank()) {
                username = target;
            }

            List<CommentItem> comments = parseComments(firstPresent(data, "comments", "commentList"));
            List<UserBookList> lists = parseBookLists(firstPresent(data, "bookLists", "lists"), username);
            int followingCount = intValue(firstPresent(data, "followingCount", "followeeCount"), 0);
            int followerCount = intValue(firstPresent(data, "followerCount", "followersCount"), 0);
            boolean followed = boolValue(firstPresent(data, "followedByCurrentUser", "followed"), false);
            return new UserProfile(username, comments, lists, followingCount, followerCount, followed);
        }, () -> super.getUserProfile(target, currentUser));
    }

    @Override
    public synchronized void sendMessage(String from, String to, String content) {
        remoteOrFallback(() -> {
            long receiverId = resolveUserId(to);
            requestData("POST", "/messages" + query(Map.of("receiverId", receiverId, "content", content)), null);
            mirror(() -> super.sendMessage(from, to, content));
            return null;
        }, () -> {
            super.sendMessage(from, to, content);
            return null;
        });
    }

    @Override
    public synchronized List<ConversationPreview> listConversations(String username) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/messages/conversations", null);
            List<ConversationPreview> list = new ArrayList<>();
            for (JsonNode item : elements(data)) {
                Long peerId = longValue(firstPresent(item, "peerId", "targetUserId", "otherUserId"), null);
                String peer = peerId == null ? text(firstPresent(item, "peer")) : resolveUsername(peerId);
                String lastMessage = text(firstPresent(item, "lastMessage", "content", "message"));
                LocalDateTime lastTime = parseDateTime(firstPresent(item, "lastTime", "createTime", "time"));
                if (peer != null) {
                    list.add(new ConversationPreview(peer, lastMessage == null ? "" : lastMessage, lastTime == null ? LocalDateTime.now() : lastTime));
                }
            }
            return list.stream().sorted((a, b) -> b.lastTime().compareTo(a.lastTime())).toList();
        }, () -> super.listConversations(username));
    }

    @Override
    public synchronized List<MessageItem> listMessages(String me, String peer) {
        return remoteOrFallback(() -> {
            long peerId = resolveUserId(peer);
            JsonNode data = requestData("GET", "/messages/conversation/" + peerId + query(Map.of("page", 1, "size", 100)), null);
            List<MessageItem> result = new ArrayList<>();
            for (JsonNode item : elements(data)) {
                String from = resolveUsername(longValue(firstPresent(item, "senderId", "fromUserId"), requireCurrentUserId()));
                String to = resolveUsername(longValue(firstPresent(item, "receiverId", "toUserId"), peerId));
                String content = text(firstPresent(item, "content"));
                LocalDateTime time = parseDateTime(firstPresent(item, "createTime", "time"));
                result.add(new MessageItem(from, to, content == null ? "" : content, time == null ? LocalDateTime.now() : time));
            }
            return result;
        }, () -> super.listMessages(me, peer));
    }

    @Override
    public synchronized List<BanInfo> listBans() {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/users/admin/bans", null);
            List<BanInfo> result = new ArrayList<>();
            for (JsonNode item : elements(data)) {
                String username = text(firstPresent(item, "username"));
                LocalDateTime until = parseDateTime(firstPresent(item, "bannedUntil", "banExpireTime"));
                if (username != null && until != null) {
                    result.add(new BanInfo(username, until));
                }
            }
            return result;
        }, () -> super.listBans());
    }

    @Override
    public synchronized void banUser(String username, LocalDateTime until) {
        remoteOrFallback(() -> {
            long userId = resolveUserId(username);
            long seconds = Math.max(1L, Duration.between(LocalDateTime.now(), until).getSeconds());
            requestData("POST", "/users/admin/" + userId + "/ban" + query(Map.of("durationSeconds", seconds)), null);
            mirror(() -> super.banUser(username, until));
            return null;
        }, () -> {
            super.banUser(username, until);
            return null;
        });
    }

    @Override
    public synchronized void unbanUser(String username) {
        remoteOrFallback(() -> {
            long userId = resolveUserId(username);
            requestData("DELETE", "/users/admin/" + userId + "/ban", null);
            mirror(() -> super.unbanUser(username));
            return null;
        }, () -> {
            super.unbanUser(username);
            return null;
        });
    }

    @Override
    public synchronized List<CommentItem> adminListAllComments() {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("GET", "/comments/admin/all", null);
            return parseComments(data);
        }, () -> super.adminListAllComments());
    }

    @Override
    public synchronized Book addBook(String title, String author, String intro, String tagsText) {
        return remoteOrFallback(() -> {
            JsonNode data = requestData("POST", "/books", json(Map.of(
                    "title", title,
                    "author", author,
                    "description", intro,
                    "cover", ""
            )));
            Book book = parseBook(firstPresent(data, "book", "data"), -1);
            if (book == null) {
                long id = longValue(firstPresent(data, "id"), System.currentTimeMillis());
                book = new Book((int) id, safe(title), safe(author), safe(intro), List.of());
            }
            mirror(() -> super.addBook(title, author, intro, tagsText));
            return book;
        }, () -> super.addBook(title, author, intro, tagsText));
    }

    @Override
    public synchronized void deleteBook(int bookId) {
        remoteOrFallback(() -> {
            requestData("DELETE", "/books/" + bookId, null);
            mirror(() -> super.deleteBook(bookId));
            return null;
        }, () -> {
            super.deleteBook(bookId);
            return null;
        });
    }

    @Override
    public synchronized Book getBook(int id) {
        return remoteOrFallback(() -> {
            // Avoid calling getBookDetail() here because MockBackendService.getBookDetail()
            // calls getBook(), which is overridden — that can produce an infinite
            // recursion when remote requests fail and fall back to super implementations.
            // Instead, fetch the minimal book info directly from the remote API.
            JsonNode data = requestData("GET", "/books/" + id, null);
            Book book = parseBook(firstPresent(data, "bookInfo", "book", "data"), id);
            if (book == null) {
                // If remote response doesn't contain book info, fall back to super
                return super.getBook(id);
            }
            return book;
        }, () -> super.getBook(id));
    }

    private List<BookCard> fetchCatalog(String titleKeyword, List<String> tags) throws IOException, InterruptedException {
        JsonNode page = requestPageData("/books" + query(Map.of("page", 1, "size", 1000)));
        List<BookCard> cards = new ArrayList<>();
        boolean needTags = tags != null && !tags.isEmpty();
        for (JsonNode item : pageRecords(page)) {
            BookCard card = parseBookCard(item, currentUsername == null ? "" : currentUsername);
            if (card == null) {
                continue;
            }

            String title = card.title();
            if (titleKeyword != null && !titleKeyword.isBlank() && !title.toLowerCase(Locale.ROOT).contains(titleKeyword.trim().toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (needTags && !card.tagsSummary().isBlank()) {
                List<String> cardTags = parseStringListFromSummary(card.tagsSummary());
                if (!cardTags.isEmpty()) {
                    boolean match = tags.stream()
                            .map(s -> s.toLowerCase(Locale.ROOT))
                            .allMatch(tag -> cardTags.stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()).contains(tag));
                    if (!match) {
                        continue;
                    }
                }
            }
            cards.add(card);
        }
        return cards;
    }

    private BookCard parseBookCard(JsonNode item, String currentUser) throws IOException, InterruptedException {
        if (item == null || item.isMissingNode() || item.isNull()) {
            return null;
        }

        JsonNode bookNode = firstPresent(item, "bookInfo", "book", "data");
        int bookId = intValue(firstPresent(item, "id", "bookId", "book_id"), -1);
        if (bookId <= 0) {
            bookId = intValue(firstPresent(bookNode, "id", "bookId", "book_id"), -1);
        }
        if (bookId <= 0) {
            return null;
        }

        String title = text(firstPresent(item, "title", "bookTitle", "name"));
        String author = text(firstPresent(item, "author", "bookAuthor"));
        List<String> tags = parseStringList(firstPresent(item, "tags", "tagNames", "tagList"));

        RatingSummary summary = parseRatingSummary(firstPresent(item, "ratings", "ratingSummary"), bookId, currentUser);

        if (title == null || title.isBlank() || author == null || author.isBlank() || tags.isEmpty() || summary == null) {
            BookDetail detail = getBookDetail(bookId, currentUser);
            if (title == null || title.isBlank()) {
                title = detail.book().title();
            }
            if (author == null || author.isBlank()) {
                author = detail.book().author();
            }
            if (tags.isEmpty()) {
                tags = detail.book().tags();
            }
            if (summary == null) {
                summary = detail.ratingSummary();
            }
        }

        return new BookCard(bookId,
                title == null ? "" : title,
                author == null ? "" : author,
                String.join(", ", tags),
                formatAvg(summary));
    }

    private List<String> parseStringListFromSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return List.of();
        }
        return List.of(summary.split(","))
                .stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    private boolean isCollected(int bookId) {
        try {
            JsonNode data = requestData("GET", "/collections/check" + query(Map.of("bookId", bookId)), null);
            return boolValue(data, false);
        } catch (Exception ex) {
            return false;
        }
    }

    private RatingSummary fetchRatingSummary(int bookId, String currentUser) {
        try {
            JsonNode data = requestData("GET", "/ratings/book/" + bookId + "/stats", null);
            return parseRatingSummary(data, bookId, currentUser);
        } catch (Exception ex) {
            return new RatingSummary(0, 0, defaultDistribution(), null);
        }
    }

    private RatingSummary parseRatingSummary(JsonNode node, int bookId, String currentUser) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        double avg = doubleValue(firstPresent(node, "avgScore", "average", "avgRating"), 0d);
        int count = intValue(firstPresent(node, "ratingCount", "count"), 0);
        Map<Integer, Integer> dist = defaultDistribution();
        JsonNode distribution = firstPresent(node, "distribution");
        if (distribution != null && distribution.isObject()) {
            distribution.fields().forEachRemaining(entry -> {
                try {
                    dist.put(Integer.parseInt(entry.getKey()), entry.getValue().asInt());
                } catch (NumberFormatException ignore) {
                }
            });
        }

        Integer myScore = null;
        try {
            JsonNode my = requestData("GET", "/ratings/my" + query(Map.of("bookId", bookId)), null);
            myScore = intNullable(firstPresent(my, "score", "data"));
        } catch (Exception ignore) {
        }
        return new RatingSummary(avg, count, dist, myScore);
    }

    private Book parseBook(JsonNode node, int fallbackId) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        int id = intValue(firstPresent(node, "id"), fallbackId);
        String title = safe(text(firstPresent(node, "title")));
        String author = safe(text(firstPresent(node, "author")));
        String intro = safe(text(firstPresent(node, "description", "intro")));
        List<String> tags = parseStringList(firstPresent(node, "tags"));
        if (tags.isEmpty()) {
            JsonNode bookInfo = firstPresent(node, "bookInfo");
            if (bookInfo != null && bookInfo != node) {
                tags = parseStringList(firstPresent(bookInfo, "tags"));
            }
        }
        return new Book(id, title, author, intro, tags);
    }

    private UserBookList parseBookListDetail(JsonNode node, String ownerFallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode listNode = firstPresent(node, "bookList", "book_list", "data");
        if (listNode == null || listNode.isMissingNode() || listNode.isNull()) {
            listNode = node;
        }
        long id = longValue(listNode, "id", longValue(node, "id", 0L));
        String owner = text(firstPresent(listNode, "owner", "username"));
        if (owner == null || owner.isBlank()) {
            owner = ownerFallback;
        }
        String title = safe(text(firstPresent(listNode, "title")));
        String intro = safe(text(firstPresent(listNode, "description", "intro")));
        List<Integer> bookIds = new ArrayList<>();
        JsonNode booksNode = firstPresent(node, "books", "items");
        if (booksNode != null && booksNode.isArray()) {
            for (JsonNode book : booksNode) {
                int bookId = intValue(firstPresent(book, "id"), -1);
                if (bookId > 0) {
                    bookIds.add(bookId);
                }
            }
        }
        JsonNode idsNode = firstPresent(listNode, "bookIds", "book_ids");
        if (bookIds.isEmpty() && idsNode != null && idsNode.isArray()) {
            for (JsonNode item : idsNode) {
                bookIds.add(item.asInt());
            }
        }
        return new UserBookList(id, owner, title, intro, bookIds);
    }

    private List<UserBookList> parseBookLists(JsonNode node, String ownerFallback) {
        List<UserBookList> result = new ArrayList<>();
        for (JsonNode item : elements(node)) {
            UserBookList list = parseBookListDetail(item, ownerFallback);
            if (list != null) {
                result.add(list);
            }
        }
        return result;
    }

    private List<CommentItem> parseComments(JsonNode node) {
        List<CommentItem> result = new ArrayList<>();
        for (JsonNode item : elements(node)) {
            CommentItem itemModel = parseComment(item, currentUsername == null ? "" : currentUsername);
            if (itemModel != null) {
                result.add(itemModel);
            }
        }
        return result;
    }

    private CommentItem parseComment(JsonNode node, String defaultUser) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        long id = longValue(node, "id", System.currentTimeMillis());
        String user = text(firstPresent(node, "user", "username", "userName"));
        if (user == null || user.isBlank()) {
            Long userId = longValue(firstPresent(node, "userId"), null);
            if (userId != null) {
                try {
                    user = resolveUsername(userId);
                } catch (Exception ignore) {
                }
            }
        }
        if (user == null || user.isBlank()) {
            user = defaultUser == null ? "" : defaultUser;
        }
        String content = safe(text(firstPresent(node, "content")));
        LocalDateTime time = parseDateTime(firstPresent(node, "time", "createTime"));
        Long parentId = longNullable(firstPresent(node, "parentId", "parentCommentId"));
        int upVotes = intValue(firstPresent(node, "upVotes", "likeCount"), 0);
        int downVotes = intValue(firstPresent(node, "downVotes", "dislikeCount"), 0);
        return new CommentItem(id, user, content, time == null ? LocalDateTime.now() : time, parentId, upVotes, downVotes);
    }

    private JsonNode requestData(String method, String path, String body) throws IOException, InterruptedException {
        String response = apiClient.send(method, path, body);
        if (response == null || response.isBlank()) {
            return mapper.nullNode();
        }
        JsonNode envelope = mapper.readTree(response);
        int code = intValue(firstPresent(envelope, "code"), 200);
        if (code != 200) {
            throw new RemoteApiException(text(firstPresent(envelope, "msg", "message")));
        }
        return firstPresent(envelope, "data", "result");
    }

    private JsonNode requestPageData(String path) throws IOException, InterruptedException {
        return requestData("GET", path, null);
    }

    private List<JsonNode> pageRecords(JsonNode pageNode) {
        JsonNode records = firstPresent(pageNode, "records", "items", "list");
        return elements(records);
    }

    private List<JsonNode> elements(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        JsonNode source = node;
        if (!source.isArray()) {
            JsonNode records = firstPresent(source, "records", "items", "list", "data");
            if (records != null) {
                source = records;
            }
        }
        if (source == null || !source.isArray()) {
            return List.of();
        }
        List<JsonNode> result = new ArrayList<>();
        source.forEach(result::add);
        return result;
    }

    private <T> T remoteOrFallback(CheckedSupplier<T> remote, Supplier<T> fallback) {
        try {
            return remote.get();
        } catch (RemoteApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return fallback.get();
        }
    }

    private void mirror(Runnable action) {
        try {
            action.run();
        } catch (Exception ignore) {
        }
    }

    private Long requireCurrentUserId() {
        if (currentUserId != null) {
            return currentUserId;
        }
        try {
            JsonNode data = requestData("GET", "/users/me", null);
            currentUserId = longValue(firstPresent(data, "id"), null);
            if (currentUsername == null) {
                currentUsername = text(firstPresent(data, "username"));
            }
        } catch (Exception ex) {
            throw new RemoteApiException("请先登录");
        }
        if (currentUserId == null) {
            throw new RemoteApiException("请先登录");
        }
        return currentUserId;
    }

    private long resolveUserId(String username) throws IOException, InterruptedException {
        if (username == null || username.isBlank()) {
            throw new RemoteApiException("用户名不能为空");
        }
        if (Objects.equals(username, currentUsername) && currentUserId != null) {
            return currentUserId;
        }
        JsonNode data = requestData("GET", "/users/profile/" + encodePathSegment(username), null);
        JsonNode userInfo = firstPresent(data, "userInfo", "user", "data");
        if (userInfo == null || userInfo.isMissingNode() || userInfo.isNull()) {
            throw new RemoteApiException("用户不存在");
        }
        Long id = longValue(userInfo, "id", null);
        if (id == null) {
            throw new RemoteApiException("用户不存在");
        }
        return id;
    }

    private String resolveUsername(long userId) throws IOException, InterruptedException {
        if (currentUserId != null && currentUserId == userId && currentUsername != null) {
            return currentUsername;
        }
        JsonNode data = requestData("GET", "/users/" + userId, null);
        JsonNode userInfo = firstPresent(data, "data", "user", "userInfo");
        if (userInfo == null || userInfo.isMissingNode() || userInfo.isNull()) {
            userInfo = data;
        }
        String username = text(firstPresent(userInfo, "username"));
        if (username == null || username.isBlank()) {
            throw new RemoteApiException("用户不存在");
        }
        return username;
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

    private List<String> parseTagsFromDetail(JsonNode node) {
        JsonNode tags = firstPresent(node, "tags");
        return parseStringList(tags);
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

    private String json(Map<String, ?> value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RemoteApiException("JSON 序列化失败");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private int intValue(JsonNode node, int fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        return node.asInt(fallback);
    }

    private Integer intNullable(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asInt();
    }

    private Long longValue(JsonNode node, String field, Long fallback) {
        return longValue(firstPresent(node, field), fallback);
    }

    private Long longValue(JsonNode node, Long fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        return node.asLong();
    }

    private long longValue(JsonNode node, String field, long fallback) {
        JsonNode child = firstPresent(node, field);
        return child == null || child.isMissingNode() || child.isNull() ? fallback : child.asLong(fallback);
    }

    private int intValue(JsonNode node, String field, int fallback) {
        JsonNode child = firstPresent(node, field);
        return child == null || child.isMissingNode() || child.isNull() ? fallback : child.asInt(fallback);
    }

    private Long longNullable(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asLong();
    }

    private double doubleValue(JsonNode node, double fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        return node.asDouble(fallback);
    }

    private boolean boolValue(JsonNode node, boolean fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        return node.asBoolean(fallback);
    }

    private LocalDateTime parseDateTime(JsonNode node) {
        String value = text(node);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String encodePathSegment(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("/", "%2F");
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private static class RemoteApiException extends RuntimeException {
        private RemoteApiException(String message) {
            super(message);
        }
    }
}
