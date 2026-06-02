package com.tihu.frontend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tihu.frontend.request.ApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteBackendServiceApiContractTest {

    @TempDir
    Path tempDir;

    private String previousStateFile;
    private String previousStrictRemote;

    @BeforeEach
    void setUp() {
        previousStateFile = System.getProperty("tihu.backend.state-file");
        System.setProperty("tihu.backend.state-file", tempDir.resolve("backend-state.json").toString());
        previousStrictRemote = System.getProperty("tihu.remote.strict");
        System.clearProperty("tihu.remote.strict");
    }

    @AfterEach
    void tearDown() {
        if (previousStateFile == null) {
            System.clearProperty("tihu.backend.state-file");
        } else {
            System.setProperty("tihu.backend.state-file", previousStateFile);
        }
        if (previousStrictRemote == null) {
            System.clearProperty("tihu.remote.strict");
        } else {
            System.setProperty("tihu.remote.strict", previousStrictRemote);
        }
    }

    @Test
    void shouldUseEnvelopeStyleApiForAuthAndCatalogOperations() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        service.registerUser("alice", "Alice123");
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("POST /users/register")));

        assertEquals(MockBackendService.Role.USER, service.login("alice", "Alice123"));
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("POST /users/login")));

        MockBackendService.Book created = service.addBook("三体", "刘慈欣", "地球文明与三体文明的接触与冲突。", "科幻 宇宙");
        assertEquals("三体", created.title());
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("POST /books")));
        assertTrue(apiClient.books.stream().anyMatch(book -> book.id == created.id() && book.title.equals("三体")));


        MockBackendService.BookDetail detail = service.getBookDetail(101, "alice");
        assertEquals(9.5, detail.ratingSummary().average(), 0.001);
        assertEquals(2, detail.ratingSummary().count());
    }

    @Test
    void shouldParseFavoritesFromCollectionRecords() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        service.registerUser("alice", "Alice123");
        service.login("alice", "Alice123");

        List<MockBackendService.BookCard> favorites = service.listFavorites("alice");
        assertEquals(1, favorites.size());
        assertEquals("初始三体", favorites.getFirst().title());
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("GET /collections?")));
    }

    @Test
    void shouldRequestBookCatalogWithServerSidePagingAndFilters() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        MockBackendService.BookListPage page = service.listBooks("三体", List.of("科幻", "宇宙"), 2, 10,
                MockBackendService.BookSortMode.RATING_DESC);

        assertEquals(2, page.page());
        assertTrue(apiClient.requestLog.stream().anyMatch(line ->
                line.startsWith("GET /books?")
                        && line.contains("page=2")
                        && line.contains("size=10")
                        && line.contains("sort=rating_desc")
                        && line.contains("keyword=%E4%B8%89%E4%BD%93")
                        && line.contains("tags=%E7%A7%91%E5%B9%BB")
                        && line.contains("tags=%E5%AE%87%E5%AE%99")));
    }

    @Test
    void shouldFallbackToLocalFilteringWhenBackendIgnoresBookFilters() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        service.addBook("围城", "钱钟书", "婚姻与人生", "文学 经典");

        MockBackendService.BookListPage page = service.listBooks("三体", List.of("科幻"), 1, 10);

        assertEquals(1, page.items().size());
        assertEquals("初始三体", page.items().getFirst().title());
    }

    @Test
    void shouldNotFallbackWhenStrictRemoteModeIsEnabled() {
        System.setProperty("tihu.remote.strict", "true");
        RemoteBackendService service = new RemoteBackendService(new FailingApiClient());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.listBooks("", List.of(), 1, 10));

        assertEquals("远程服务不可用", ex.getMessage());
    }

    @Test
    void shouldLoadBookCommentsWithVotesAndReplies() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        MockBackendService.BookDetail detail = service.getBookDetail(100, "alice");

        assertEquals(1, detail.comments().size());
        assertEquals(1, detail.replies().size());
        assertEquals(3, detail.comments().getFirst().upVotes());
        assertEquals(1, detail.comments().getFirst().downVotes());
        assertEquals(detail.comments().getFirst().id(), detail.replies().getFirst().parentId());
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("GET /comments/book/100?")));
    }

    @Test
    void shouldSurfaceRemoteWithdrawPermissionErrors() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.deleteOwnComment(100, 999, "alice"));

        assertEquals("只能撤回自己的评论", ex.getMessage());
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.equals("DELETE /comments/999")));
    }

    @Test
    void shouldSurfaceRemoteBanLoginErrorsWithUnbanTime() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.login("banned", "Banned123"));

        assertEquals("您已被封禁，解封时间是 2026-06-02T12:00:00", ex.getMessage());
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.equals("POST /users/login")));
    }

    @Test
    void shouldCallBanAndUnbanApisByUsername() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        service.banUser("alice", java.time.LocalDateTime.parse("2026-06-02T12:00:00"));
        service.unbanUser("alice");

        assertTrue(apiClient.requestLog.stream().anyMatch(line ->
                line.startsWith("POST /users/ban?")
                        && line.contains("username=alice")
                        && line.contains("until=2026-06-02T12%3A00")));
        assertTrue(apiClient.requestLog.stream().anyMatch(line ->
                line.equals("POST /users/unban?username=alice")));
    }

    @Test
    void shouldLoadOtherUserProfileAndFollowListsByUsername() {
        FakeApiClient apiClient = new FakeApiClient();
        RemoteBackendService service = new RemoteBackendService(apiClient);

        service.registerUser("alice", "Alice123");
        service.registerUser("bob", "Bob12345");
        service.login("alice", "Alice123");

        MockBackendService.UserProfile profile = service.getUserProfile("bob", "alice");
        assertEquals("bob", profile.username());
        assertEquals(1, profile.followingCount());
        assertEquals(1, profile.followerCount());
        assertTrue(profile.followedByCurrentUser());
        assertEquals(1, profile.bookLists().size());
        assertEquals(1, profile.comments().size());
        assertEquals("科幻精选", profile.bookLists().getFirst().title());

        List<MockBackendService.FollowItem> followers = service.listFollowers("bob");
        List<MockBackendService.FollowItem> following = service.listFollowing("bob");
        assertEquals(List.of("alice"), followers.stream().map(MockBackendService.FollowItem::username).toList());
        assertEquals(List.of("carol"), following.stream().map(MockBackendService.FollowItem::username).toList());

        service.follow("alice", "bob");
        service.unfollow("alice", "bob");

        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("GET /users/profile/bob")));
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("GET /follows/user/2/followers?")));
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("GET /follows/user/2/followees?")));
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("POST /follows?followeeId=2")));
        assertTrue(apiClient.requestLog.stream().anyMatch(line -> line.startsWith("DELETE /follows?followeeId=2")));
    }

    private static final class FailingApiClient extends ApiClient {
        private FailingApiClient() {
            super("http://test/api");
        }

        @Override
        public String send(String method, String path, String body) {
            throw new IllegalStateException("远程服务不可用");
        }
    }

    private static final class FakeApiClient extends ApiClient {
        private final ObjectMapper mapper = new ObjectMapper();
        private final List<String> requestLog = new ArrayList<>();
        private final Map<String, UserRecord> users = new LinkedHashMap<>();
        private final List<BookRecord> books = new ArrayList<>();
        private long userSeq = 1;
        private long bookSeq = 100;

        private FakeApiClient() {
            super("http://test/api");
            books.add(new BookRecord(bookSeq++, "初始三体", "刘慈欣", "预置图书", List.of("科幻", "宇宙")));
        }

        @Override
        public String send(String method, String path, String body) {
            requestLog.add(method + " " + path);
            return switch (method.toUpperCase() + " " + path) {
                case "POST /users/register" -> register(body);
                case "POST /users/login" -> login(body);
                case "POST /books" -> createBook(body);
                case "DELETE /comments/999" -> envelope(403, "只能撤回自己的评论", null);
                default -> path.startsWith("/books?") ? listBooks(path)
                        : path.startsWith("/collections?") ? listCollections()
                        : path.startsWith("/comments/book/") ? listBookComments()
                        : path.startsWith("/users/profile/") ? getUserProfile(path)
                        : path.startsWith("/follows/user/") ? listUserFollows(path)
                        : path.startsWith("/follows/followers?") ? listFollowers(path)
                        : path.startsWith("/follows/followees?") ? listFollowees(path)
                        : path.startsWith("/books/") ? getBook(path)
                        : envelope(200, "OK", null);
            };
        }

        private String register(String body) {
            JsonNode node = read(body);
            String username = node.path("username").asText();
            String password = node.path("password").asText();
            users.put(username, new UserRecord(userSeq++, username, password, "USER"));
            return envelope(200, "OK", null);
        }

        private String login(String body) {
            JsonNode node = read(body);
            String username = node.path("username").asText();
            String password = node.path("password").asText();
            if ("banned".equals(username)) {
                return envelope(403, "该用户已被封禁", Map.of("bannedUntil", "2026-06-02T12:00:00"));
            }
            UserRecord user = users.get(username);
            if (user == null || !user.password.equals(password)) {
                return envelope(400, "用户名或密码错误", null);
            }
            return envelope(200, "OK", Map.of("userInfo", Map.of(
                    "id", user.id,
                    "username", user.username,
                    "role", user.role
            )));
        }

        private String createBook(String body) {
            JsonNode node = read(body);
            BookRecord book = new BookRecord(bookSeq++, node.path("title").asText(), node.path("author").asText(),
                    node.path("description").asText(), toTags(node.path("tags")));
            books.add(book);
            return envelope(200, "OK", Map.of("book", book.toMap()));
        }

        private String listBooks(String path) {
            int current = parsePositiveInt(queryValue(path, "page"), 1);
            int size = parsePositiveInt(queryValue(path, "size"), 10);
            List<Map<String, Object>> records = books.stream().map(BookRecord::toCardMap).toList();
            return envelope(200, "OK", Map.of(
                    "records", records,
                    "total", records.size(),
                    "pages", Math.max(1, (int) Math.ceil(records.size() / (double) size)),
                    "current", current,
                    "size", size
            ));
        }

        private String listCollections() {
            Map<String, Object> collection = new LinkedHashMap<>();
            collection.put("owner", "alice");
            collection.put("bookId", 101);
            collection.put("bookInfo", books.getFirst().toMap());
            collection.put("ratings", Map.of(
                    "avgScore", 8.8,
                    "ratingCount", 5,
                    "distribution", new LinkedHashMap<>()
            ));
            return envelope(200, "OK", Map.of(
                    "records", List.of(collection),
                    "total", 1,
                    "pages", 1,
                    "current", 1,
                    "size", 10
            ));
        }

        private String getBook(String path) {
            long id = Long.parseLong(path.substring("/books/".length()));
            BookRecord book = books.stream().filter(item -> item.id == id).findFirst().orElseThrow();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("bookInfo", book.toMap());
            data.put("ratings", Map.of(
                    "avgScore", 9.5,
                    "ratingCount", 2,
                    "distribution", new LinkedHashMap<>()
            ));
            data.put("comments", List.of());
            data.put("replies", List.of());
            return envelope(200, "OK", data);
        }

        private String listBookComments() {
            return envelope(200, "OK", Map.of(
                    "records", List.of(
                            Map.of(
                                    "id", 501,
                                    "username", "alice",
                                    "content", "这本书不错",
                                    "createTime", "2026-05-31T12:00:00",
                                    "upVotes", 3,
                                    "downVotes", 1
                            ),
                            Map.of(
                                    "id", 502,
                                    "username", "bob",
                                    "content", "同意",
                                    "createTime", "2026-05-31T12:05:00",
                                    "parentCommentId", 501,
                                    "upVotes", 2,
                                    "downVotes", 0
                            )
                    ),
                    "total", 2,
                    "pages", 1,
                    "current", 1,
                    "size", 100
            ));
        }

        private String getUserProfile(String path) {
            String username = path.substring("/users/profile/".length());
            UserRecord user = users.get(username);
            if (user == null) {
                return envelope(404, "用户不存在", null);
            }
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("userInfo", Map.of(
                    "id", user.id,
                    "username", user.username,
                    "avatar", "https://example.com/avatar.png",
                    "bio", "读书爱好者"
            ));
            profile.put("followingCount", 1);
            profile.put("followerCount", 1);
            profile.put("followedByCurrentUser", "bob".equals(user.username));
            profile.put("comments", List.of(Map.of(
                    "id", 501,
                    "userId", user.id,
                    "username", user.username,
                    "content", "这本书很不错",
                    "createTime", "2026-05-25T13:45:00"
            )));
            profile.put("bookLists", List.of(Map.of(
                    "id", 801,
                    "userId", user.id,
                    "title", "科幻精选",
                    "description", "公开书单"
            )));
            return envelope(200, "OK", profile);
        }

        private String listFollowers(String path) {
            return envelope(200, "OK", Map.of(
                    "records", List.of(Map.of("follower", Map.of("username", "alice"))),
                    "total", 1,
                    "pages", 1,
                    "current", 1,
                    "size", 10
            ));
        }

        private String listFollowees(String path) {
            return envelope(200, "OK", Map.of(
                    "records", List.of(Map.of("followee", Map.of("username", "carol"))),
                    "total", 1,
                    "pages", 1,
                    "current", 1,
                    "size", 10
            ));
        }

        private String listUserFollows(String path) {
            if (path.contains("/followers?")) {
                return listFollowers(path);
            }
            if (path.contains("/followees?")) {
                return listFollowees(path);
            }
            return envelope(404, "not found", null);
        }

        private String queryValue(String path, String key) {
            int queryIndex = path.indexOf('?');
            if (queryIndex < 0 || queryIndex == path.length() - 1) {
                return "";
            }
            String query = path.substring(queryIndex + 1);
            for (String part : query.split("&")) {
                String[] entry = part.split("=", 2);
                if (entry.length == 2 && key.equals(entry[0])) {
                    return entry[1];
                }
            }
            return "";
        }

        private int parsePositiveInt(String value, int defaultValue) {
            try {
                int parsed = Integer.parseInt(value);
                return parsed > 0 ? parsed : defaultValue;
            } catch (Exception ex) {
                return defaultValue;
            }
        }

        private JsonNode read(String body) {
            try {
                return mapper.readTree(body == null ? "{}" : body);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private List<String> toTags(JsonNode node) {
            List<String> tags = new ArrayList<>();
            for (JsonNode item : node) {
                tags.add(item.asText());
            }
            return tags;
        }

        private String envelope(int code, String message, Object data) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", code);
            response.put("message", message);
            response.put("data", data);
            try {
                return mapper.writeValueAsString(response);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private record UserRecord(long id, String username, String password, String role) {
        }

        private record BookRecord(long id, String title, String author, String description, List<String> tags) {
            Map<String, Object> toMap() {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", id);
                map.put("title", title);
                map.put("author", author);
                map.put("description", description);
                map.put("tags", tags);
                return map;
            }

            Map<String, Object> toCardMap() {
                Map<String, Object> map = new LinkedHashMap<>(toMap());
                map.put("averageScore", 0);
                Map<String, Object> ratings = new LinkedHashMap<>();
                ratings.put("avgScore", 9.5);
                ratings.put("ratingCount", 2);
                ratings.put("distribution", new LinkedHashMap<>());
                map.put("ratings", ratings);
                return map;
            }
        }
    }
}



