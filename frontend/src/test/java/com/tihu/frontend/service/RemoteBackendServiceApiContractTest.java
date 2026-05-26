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
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteBackendServiceApiContractTest {

    @TempDir
    Path tempDir;

    private String previousStateFile;

    @BeforeEach
    void setUp() {
        previousStateFile = System.getProperty("tihu.backend.state-file");
        System.setProperty("tihu.backend.state-file", tempDir.resolve("backend-state.json").toString());
    }

    @AfterEach
    void tearDown() {
        if (previousStateFile == null) {
            System.clearProperty("tihu.backend.state-file");
        } else {
            System.setProperty("tihu.backend.state-file", previousStateFile);
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
                default -> path.startsWith("/books?") ? listBooks()
                        : path.startsWith("/collections?") ? listCollections()
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

        private String listBooks() {
            List<Map<String, Object>> records = books.stream().map(BookRecord::toCardMap).toList();
            return envelope(200, "OK", Map.of(
                    "records", records,
                    "total", records.size(),
                    "pages", 1,
                    "current", 1,
                    "size", 10
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



