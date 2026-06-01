package com.tihu.frontend.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MockBackendService {

    public enum BookSortMode {
        DEFAULT,
        RATING_DESC,
        TITLE_ASC
    }

    public enum Role {
        USER,
        ADMIN
    }

    public record Book(long id, String title, String author, String intro, List<String> tags) {
    }

    public record BookListPage(List<BookCard> items, int page, int totalPages, int totalItems) {
    }

    public record BookCard(long id, String title, String author, String tagsSummary, String averageScoreText) {
    }

    public record RatingSummary(double average, int count, Map<Integer, Integer> distribution, Integer myScore) {
    }

    public record CommentItem(long id, String user, String content, LocalDateTime time, Long parentId, int upVotes, int downVotes) {
    }

    public record BookDetail(Book book, RatingSummary ratingSummary, List<CommentItem> comments, List<CommentItem> replies, int favoriteCount) {
    }

    public record UserBookList(long id, String owner, String title, String intro, List<Long> bookIds) {
    }

    public record UserProfile(String username, List<CommentItem> comments, List<UserBookList> bookLists, int followingCount,
                              int followerCount, boolean followedByCurrentUser) {
    }

    public record FollowItem(String username) {
    }

    public record ConversationPreview(String peer, String lastMessage, LocalDateTime lastTime) {
    }

    public record MessageItem(String from, String to, String content, LocalDateTime time) {
    }

    public record BanInfo(String username, LocalDateTime bannedUntil) {
    }

    private static class UserEntity {
        private String username;
        private String password;
        private Role role;
        private LocalDateTime bannedUntil;

        private UserEntity(String username, String password, Role role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }

    private final AtomicInteger bookIdSeq = new AtomicInteger(100);
    private final AtomicLong commentIdSeq = new AtomicLong(1000);
    private final AtomicLong listIdSeq = new AtomicLong(2000);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path stateFile;

    private final Map<String, UserEntity> users = new LinkedHashMap<>();
    private final Map<Long, Book> books = new LinkedHashMap<>();
    private final Map<Long, Map<String, Integer>> ratingMap = new HashMap<>();
    private final Map<Long, List<CommentItem>> commentMap = new HashMap<>();
    private final Map<Long, Map<String, Integer>> voteMap = new HashMap<>();
    private final Map<String, Set<Long>> favoriteMap = new HashMap<>();
    private final Map<String, List<UserBookList>> userBookLists = new HashMap<>();
    private final Map<String, Set<String>> followingMap = new HashMap<>();
    private final Map<String, List<MessageItem>> messageMap = new HashMap<>();

    public MockBackendService() {
        this(resolveStateFile());
    }

    MockBackendService(Path stateFile) {
        this.stateFile = stateFile == null ? resolveStateFile() : stateFile.toAbsolutePath().normalize();
        if (!loadState()) {
            seed();
            persistState();
        }
    }

    public void logout() {
        // 远程实现可覆盖该方法以清理 token / 会话信息
    }

    public String token() {
        return null;
    }

    public synchronized Role login(String username, String password) {
        UserEntity user = users.get(username);
        if (user == null || !Objects.equals(user.password, password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.bannedUntil != null && user.bannedUntil.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("账号已被封禁至 " + user.bannedUntil);
        }
        return user.role;
    }

    public synchronized void registerUser(String username, String password) {
        validateUsername(username);
        validatePassword(password);
        ensureUserNotExists(username);
        users.put(username, new UserEntity(username, password, Role.USER));
        favoriteMap.put(username, new HashSet<>());
        userBookLists.put(username, new ArrayList<>());
        followingMap.put(username, new HashSet<>());
        persistState();
    }

    public synchronized void registerAdmin(String username, String password, String inviteCode) {
        if (!"123456".equals(inviteCode)) {
            throw new IllegalArgumentException("管理员邀请码错误");
        }
        validateUsername(username);
        validatePassword(password);
        ensureUserNotExists(username);
        users.put(username, new UserEntity(username, password, Role.ADMIN));
        favoriteMap.put(username, new HashSet<>());
        userBookLists.put(username, new ArrayList<>());
        followingMap.put(username, new HashSet<>());
        persistState();
    }

    public synchronized void updateProfile(String currentUsername, String newUsername, String newPassword) {
        UserEntity user = getRequiredUser(currentUsername);
        if (newUsername != null && !newUsername.isBlank() && !currentUsername.equals(newUsername)) {
            validateUsername(newUsername);
            ensureUserNotExists(newUsername);
            users.remove(currentUsername);
            user.username = newUsername;
            users.put(newUsername, user);
            moveUserData(currentUsername, newUsername);
        }
        if (newPassword != null && !newPassword.isBlank()) {
            validatePassword(newPassword);
            user.password = newPassword;
        }
        persistState();
    }

    public synchronized BookListPage listBooks(String titleKeyword, List<String> tags, int page, int pageSize) {
        return listBooks(titleKeyword, tags, page, pageSize, BookSortMode.DEFAULT);
    }

    public synchronized BookListPage listBooks(String titleKeyword, List<String> tags, int page, int pageSize, BookSortMode sortMode) {
        List<Book> filtered = books.values().stream()
                .filter(book -> containsKeyword(book.title(), titleKeyword))
                .filter(book -> containsAllTags(book.tags(), tags))
                .toList();

        filtered = applyBookSorting(filtered, sortMode);

        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) safePageSize));
        int from = Math.min((safePage - 1) * safePageSize, total);
        int to = Math.min(from + safePageSize, total);

        List<BookCard> items = filtered.subList(from, to).stream().map(this::toCard).toList();
        return new BookListPage(items, safePage, totalPages, total);
    }

    public synchronized BookDetail getBookDetail(long bookId, String currentUser) {
        return buildBookDetail(bookId, currentUser);
    }

    public synchronized BookDetail getBookDetail(int bookId, String currentUser) {
        return buildBookDetail((long) bookId, currentUser);
    }

    public synchronized void rateBook(long bookId, String username, int score) {
        rateBookInternal(bookId, username, score);
        persistState();
    }

    public synchronized void rateBook(int bookId, String username, int score) {
        rateBook((long) bookId, username, score);
    }

    public synchronized void toggleFavorite(String username, long bookId) {
        toggleFavoriteInternal(username, bookId);
        persistState();
    }

    public synchronized void toggleFavorite(String username, int bookId) {
        toggleFavorite(username, (long) bookId);
    }

    public synchronized boolean isCollected(String username, long bookId) {
        getRequiredUser(username);
        return favoriteMap.getOrDefault(username, Set.of()).contains(bookId);
    }

    public synchronized boolean isCollected(String username, int bookId) {
        return isCollected(username, (long) bookId);
    }

    public synchronized List<BookCard> listFavorites(String username) {
        getRequiredUser(username);
        return favoriteMap.getOrDefault(username, Set.of()).stream()
                .map(this::getBook)
                .sorted(Comparator.comparing(Book::title))
                .map(this::toCard)
                .toList();
    }

    public synchronized CommentItem addComment(long bookId, String username, String content, Long parentCommentId) {
        CommentItem item = addCommentInternal(bookId, username, content, parentCommentId);
        persistState();
        return item;
    }

    public synchronized CommentItem addComment(int bookId, String username, String content, Long parentCommentId) {
        return addComment((long) bookId, username, content, parentCommentId);
    }

    public synchronized void updateBook(long bookId, String title, String author, String intro, String tagsText) {
        updateBookInternal(bookId, title, author, intro, tagsText);
        persistState();
    }

    public synchronized void updateBook(int bookId, String title, String author, String intro, String tagsText) {
        updateBook((long) bookId, title, author, intro, tagsText);
    }

    public synchronized void deleteOwnComment(long bookId, long commentId, String username) {
        deleteOwnCommentInternal(bookId, commentId, username);
        persistState();
    }

    public synchronized void deleteOwnComment(int bookId, long commentId, String username) {
        deleteOwnComment((long) bookId, commentId, username);
    }

    public synchronized void adminDeleteComment(long bookId, long commentId) {
        adminDeleteCommentInternal(bookId, commentId);
        persistState();
    }

    public synchronized void adminDeleteComment(int bookId, long commentId) {
        adminDeleteComment((long) bookId, commentId);
    }

    public synchronized void adminDeleteComment(long commentId) {
        deleteCommentByIdInternal(commentId);
        persistState();
    }

    public synchronized void voteComment(long commentId, String username, int target) {
        if (target != -1 && target != 0 && target != 1) {
            throw new IllegalArgumentException("投票参数错误");
        }
        getRequiredUser(username);

        CommentItem hit = null;
        long hitBookId = -1;
        for (Map.Entry<Long, List<CommentItem>> entry : commentMap.entrySet()) {
            for (CommentItem item : entry.getValue()) {
                if (item.id() == commentId) {
                    hit = item;
                    hitBookId = entry.getKey();
                    break;
                }
            }
        }
        if (hit == null) {
            throw new IllegalArgumentException("评论不存在");
        }

        Map<String, Integer> userVote = voteMap.computeIfAbsent(commentId, key -> new HashMap<>());
        int oldVote = userVote.getOrDefault(username, 0);
        if (oldVote == target) {
            target = 0;
        }
        userVote.put(username, target);

        List<CommentItem> list = commentMap.getOrDefault(hitBookId, new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            CommentItem item = list.get(i);
            if (item.id() == commentId) {
                int up = item.upVotes();
                int down = item.downVotes();
                if (oldVote == 1) {
                    up--;
                } else if (oldVote == -1) {
                    down--;
                }
                if (target == 1) {
                    up++;
                } else if (target == -1) {
                    down++;
                }
                list.set(i, new CommentItem(item.id(), item.user(), item.content(), item.time(), item.parentId(), up, down));
                break;
            }
        }
        persistState();
    }

    public synchronized List<UserBookList> listBookLists(String username) {
        return new ArrayList<>(userBookLists.getOrDefault(username, List.of()));
    }

    public synchronized UserBookList createBookList(String username, String title, String intro) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("书单标题不能为空");
        }
        UserBookList list = new UserBookList(listIdSeq.incrementAndGet(), username, title.trim(),
                intro == null ? "" : intro.trim(), new ArrayList<>());
        userBookLists.computeIfAbsent(username, key -> new ArrayList<>()).add(list);
        persistState();
        return list;
    }

    public synchronized void deleteBookList(String username, long listId) {
        userBookLists.getOrDefault(username, new ArrayList<>()).removeIf(item -> item.id() == listId);
        persistState();
    }

    public synchronized UserBookList getBookList(String owner, long listId) {
        return userBookLists.getOrDefault(owner, List.of()).stream()
                .filter(item -> item.id() == listId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("书单不存在"));
    }

    public synchronized void addBookToBookList(String username, long listId, long bookId) {
        addBookToBookListInternal(username, Math.toIntExact(listId), Math.toIntExact(bookId));
        persistState();
    }

    public synchronized void addBookToBookList(String username, long listId, int bookId) {
        addBookToBookList(username, listId, (long) bookId);
    }

    public synchronized void removeBookFromBookList(String username, long listId, long bookId) {
        removeBookFromBookListInternal(username, Math.toIntExact(listId), Math.toIntExact(bookId));
        persistState();
    }

    public synchronized void removeBookFromBookList(String username, long listId, int bookId) {
        removeBookFromBookList(username, listId, (long) bookId);
    }

    public synchronized void follow(String me, String target) {
        if (Objects.equals(me, target)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        getRequiredUser(me);
        getRequiredUser(target);
        followingMap.computeIfAbsent(me, key -> new HashSet<>()).add(target);
        persistState();
    }

    public synchronized void unfollow(String me, String target) {
        followingMap.computeIfAbsent(me, key -> new HashSet<>()).remove(target);
        persistState();
    }

    public synchronized List<FollowItem> listFollowing(String username) {
        return followingMap.getOrDefault(username, Set.of()).stream().sorted().map(FollowItem::new).toList();
    }

    public synchronized List<FollowItem> listFollowers(String username) {
        return followingMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(username))
                .map(Map.Entry::getKey)
                .sorted()
                .map(FollowItem::new)
                .toList();
    }

    public synchronized UserProfile getUserProfile(String target, String currentUser) {
        getRequiredUser(target);
        List<CommentItem> comments = commentMap.values().stream()
                .flatMap(Collection::stream)
                .filter(item -> item.user().equals(target))
                .sorted(Comparator.comparing(CommentItem::time).reversed())
                .toList();
        List<UserBookList> lists = listBookLists(target);
        int followingCount = listFollowing(target).size();
        int followerCount = listFollowers(target).size();
        boolean followed = followingMap.getOrDefault(currentUser, Set.of()).contains(target);
        return new UserProfile(target, comments, lists, followingCount, followerCount, followed);
    }

    public synchronized void sendMessage(String from, String to, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        getRequiredUser(from);
        getRequiredUser(to);
        String key = conversationKey(from, to);
        messageMap.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new MessageItem(from, to, content.trim(), LocalDateTime.now()));
        persistState();
    }

    public synchronized List<ConversationPreview> listConversations(String username) {
        return messageMap.entrySet().stream()
                .filter(entry -> entry.getKey().contains("#" + username + "#"))
                .map(entry -> {
                    List<MessageItem> list = entry.getValue();
                    MessageItem last = list.get(list.size() - 1);
                    String[] usersPair = entry.getKey().split("#");
                    String peer = usersPair[1].equals(username) ? usersPair[2] : usersPair[1];
                    return new ConversationPreview(peer, last.content(), last.time());
                })
                .sorted(Comparator.comparing(ConversationPreview::lastTime).reversed())
                .toList();
    }

    public synchronized List<MessageItem> listMessages(String me, String peer) {
        return new ArrayList<>(messageMap.getOrDefault(conversationKey(me, peer), List.of()));
    }

    public synchronized List<BanInfo> listBans() {
        return users.values().stream()
                .filter(item -> item.bannedUntil != null && item.bannedUntil.isAfter(LocalDateTime.now()))
                .map(item -> new BanInfo(item.username, item.bannedUntil))
                .sorted(Comparator.comparing(BanInfo::bannedUntil))
                .toList();
    }

    public synchronized void banUser(String username, LocalDateTime until) {
        UserEntity user = getRequiredUser(username);
        user.bannedUntil = until;
        persistState();
    }

    public synchronized void unbanUser(String username) {
        UserEntity user = getRequiredUser(username);
        user.bannedUntil = null;
        persistState();
    }

    public synchronized List<CommentItem> adminListAllComments() {
        return commentMap.values().stream().flatMap(Collection::stream)
                .sorted(Comparator.comparing(CommentItem::time).reversed())
                .toList();
    }

    public synchronized Book addBook(String title, String author, String intro, String tagsText) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("书名必填");
        }
        if (books.values().stream().anyMatch(book -> book.title().equals(title.trim()))) {
            throw new IllegalStateException("书名已存在");
        }
        Book book = new Book(bookIdSeq.incrementAndGet(), title.trim(), valueOrEmpty(author), valueOrEmpty(intro),
                parseTags(tagsText));
        books.put(book.id(), book);
        persistState();
        return book;
    }

    public synchronized void deleteBook(int bookId) {
        books.remove((long) bookId);
        ratingMap.remove(bookId);
        commentMap.remove(bookId);
        favoriteMap.values().forEach(set -> set.remove((long) bookId));
        userBookLists.values().forEach(lists -> lists.forEach(list -> list.bookIds().remove(Long.valueOf(bookId))));
        persistState();
    }

    public synchronized void deleteBook(long bookId) {
        books.remove(bookId);
        ratingMap.remove(Math.toIntExact(bookId));
        commentMap.remove(Math.toIntExact(bookId));
        favoriteMap.values().forEach(set -> set.remove(bookId));
        userBookLists.values().forEach(lists -> lists.forEach(list -> list.bookIds().remove(bookId)));
        persistState();
    }

    private BookCard toCard(Book book) {
        double avg = averageScore(book.id());
        String avgText = avg > 0 ? String.format(Locale.US, "%.1f", avg) : "暂无评分";
        String tags = String.join(" ", book.tags());
        return new BookCard(book.id(), book.title(), book.author(), tags, avgText);
    }

    private List<Book> applyBookSorting(List<Book> books, BookSortMode sortMode) {
        if (books == null || books.size() <= 1) {
            return books == null ? List.of() : books;
        }
        if (sortMode == null || sortMode == BookSortMode.DEFAULT) {
            return books;
        }
        Comparator<Book> comparator = switch (sortMode) {
            case RATING_DESC -> Comparator.comparingDouble((Book b) -> averageScore(b.id())).reversed()
                    .thenComparing(Book::title)
                    .thenComparingLong(Book::id);
            case TITLE_ASC -> Comparator.comparing(Book::title, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Book::title)
                    .thenComparingLong(Book::id);
            case DEFAULT -> null;
        };
        if (comparator == null) {
            return books;
        }
        return books.stream().sorted(comparator).toList();
    }

    private RatingSummary ratingSummary(long bookId, String username) {
        Map<String, Integer> map = ratingMap.getOrDefault(bookId, Map.of());
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int i = 1; i <= 10; i++) {
            distribution.put(i, 0);
        }
        map.values().forEach(score -> distribution.put(score, distribution.get(score) + 1));

        double avg = averageScore(bookId);
        Integer myScore = map.get(username);
        return new RatingSummary(avg, map.size(), distribution, myScore);
    }

    private boolean isTopLevelComment(long bookId, long commentId) {
        return commentMap.getOrDefault(bookId, List.of()).stream()
                .anyMatch(item -> item.id() == commentId && item.parentId() == null);
    }

    private double averageScore(long bookId) {
        Map<String, Integer> map = ratingMap.getOrDefault(bookId, Map.of());
        if (map.isEmpty()) {
            return 0;
        }
        return map.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public synchronized Book getBook(long id) {
        return getBookInternal(id);
    }

    public synchronized Book getBook(int id) {
        return getBookInternal((long) id);
    }

    private Book getBookInternal(long id) {
        Book book = books.get(id);
        if (book == null) {
            throw new IllegalArgumentException("图书不存在");
        }
        return book;
    }

    private BookDetail buildBookDetail(long bookId, String currentUser) {
        Book book = getBookInternal(bookId);
        RatingSummary summary = ratingSummary(bookId, currentUser);
        int favoriteCount = (int) favoriteMap.values().stream()
                .filter(set -> set.contains(bookId))
                .count();

        List<CommentItem> allComments = commentMap.getOrDefault(bookId, List.of()).stream()
                .sorted(Comparator.comparing(CommentItem::time).reversed())
                .toList();

        List<CommentItem> comments = allComments.stream().filter(item -> item.parentId() == null).toList();
        List<CommentItem> replies = allComments.stream().filter(item -> item.parentId() != null).toList();
        return new BookDetail(book, summary, comments, replies, favoriteCount);
    }

    private void rateBookInternal(long bookId, String username, int score) {
        if (score < 1 || score > 10) {
            throw new IllegalArgumentException("评分必须在 1~10");
        }
        getRequiredUser(username);
        getBookInternal(bookId);
        ratingMap.computeIfAbsent(bookId, key -> new HashMap<>()).put(username, score);
    }

    private void toggleFavoriteInternal(String username, long bookId) {
        getRequiredUser(username);
        getBookInternal(bookId);
        Set<Long> set = favoriteMap.computeIfAbsent(username, key -> new HashSet<>());
        if (!set.add(bookId)) {
            set.remove(bookId);
        }
    }

    private CommentItem addCommentInternal(long bookId, String username, String content, Long parentCommentId) {
        getRequiredUser(username);
        getBookInternal(bookId);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论不能为空");
        }
        if (content.length() > 200) {
            throw new IllegalArgumentException("评论最多 200 字");
        }
        if (parentCommentId != null && !isTopLevelComment(bookId, parentCommentId)) {
            throw new IllegalArgumentException("只能回复一级评论");
        }
        CommentItem item = new CommentItem(commentIdSeq.incrementAndGet(), username, content.trim(), LocalDateTime.now(),
                parentCommentId, 0, 0);
        commentMap.computeIfAbsent(bookId, key -> new ArrayList<>()).add(item);
        return item;
    }

    private void updateBookInternal(long bookId, String title, String author, String intro, String tagsText) {
        getBookInternal(bookId);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("书名必填");
        }
        String normalizedTitle = title.trim();
        boolean duplicated = books.values().stream()
                .anyMatch(book -> book.id() != bookId && book.title().equals(normalizedTitle));
        if (duplicated) {
            throw new IllegalStateException("书名已存在");
        }
        books.put(bookId, new Book(bookId, normalizedTitle, valueOrEmpty(author), valueOrEmpty(intro), parseTags(tagsText)));
    }

    private void deleteOwnCommentInternal(long bookId, long commentId, String username) {
        List<CommentItem> list = commentMap.getOrDefault(bookId, new ArrayList<>());
        boolean exists = list.stream().anyMatch(item -> item.id() == commentId);
        if (!exists) {
            throw new IllegalArgumentException("评论不存在");
        }
        boolean canDelete = list.stream().anyMatch(item -> item.id() == commentId && item.user().equals(username));
        if (!canDelete) {
            throw new IllegalStateException("只能撤回自己的评论");
        }
        deleteCommentAndReplies(list, commentId);
    }

    private void adminDeleteCommentInternal(long bookId, long commentId) {
        List<CommentItem> list = commentMap.getOrDefault(bookId, new ArrayList<>());
        if (!deleteCommentAndReplies(list, commentId)) {
            throw new IllegalArgumentException("评论不存在");
        }
    }

    private void deleteCommentByIdInternal(long commentId) {
        for (List<CommentItem> list : commentMap.values()) {
            if (deleteCommentAndReplies(list, commentId)) {
                return;
            }
        }
        throw new IllegalArgumentException("评论不存在");
    }

    private boolean deleteCommentAndReplies(List<CommentItem> list, long commentId) {
        Set<Long> removedIds = list.stream()
                .filter(item -> item.id() == commentId || Objects.equals(item.parentId(), commentId))
                .map(CommentItem::id)
                .collect(Collectors.toSet());
        if (removedIds.isEmpty()) {
            return false;
        }
        boolean removed = list.removeIf(item -> item.id() == commentId || Objects.equals(item.parentId(), commentId));
        if (removed) {
            removedIds.forEach(voteMap::remove);
        }
        return removed;
    }

    private void addBookToBookListInternal(String username, int listId, int bookId) {
        UserBookList item = getBookList(username, listId);
        if (item.bookIds().contains((long) bookId)) {
            throw new IllegalStateException("同一本书不能重复加入书单");
        }
        item.bookIds().add((long) bookId);
    }

    private void removeBookFromBookListInternal(String username, int listId, int bookId) {
        UserBookList item = getBookList(username, listId);
        item.bookIds().remove(Long.valueOf(bookId));
    }

    private void ensureUserNotExists(String username) {
        if (users.containsKey(username)) {
            throw new IllegalStateException("用户名已存在");
        }
    }

    private UserEntity getRequiredUser(String username) {
        UserEntity user = users.get(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private void validateUsername(String username) {
        if (username == null || username.length() < 2 || username.length() > 10) {
            throw new IllegalArgumentException("用户名长度需为 2~10");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 12) {
            throw new IllegalArgumentException("密码长度需为 6~12");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("密码需同时包含英文和数字");
        }
    }

    private boolean containsKeyword(String text, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean containsAllTags(List<String> sourceTags, List<String> needTags) {
        if (needTags == null || needTags.isEmpty()) {
            return true;
        }
        Set<String> src = sourceTags.stream().map(tag -> tag.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        return needTags.stream().map(tag -> tag.toLowerCase(Locale.ROOT)).allMatch(src::contains);
    }

    private List<String> parseTags(String tagsText) {
        if (tagsText == null || tagsText.isBlank()) {
            return List.of();
        }
        return List.of(tagsText.trim().split("\\s+" )).stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private String conversationKey(String a, String b) {
        if (a.compareTo(b) < 0) {
            return "#" + a + "#" + b;
        }
        return "#" + b + "#" + a;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void moveUserData(String from, String to) {
        favoriteMap.put(to, favoriteMap.remove(from));
        userBookLists.put(to, userBookLists.remove(from));
        followingMap.put(to, followingMap.remove(from));

        for (Set<String> following : followingMap.values()) {
            if (following.remove(from)) {
                following.add(to);
            }
        }

        ratingMap.values().forEach(map -> {
            Integer score = map.remove(from);
            if (score != null) {
                map.put(to, score);
            }
        });

        commentMap.replaceAll((bookId, list) -> list.stream().map(item -> {
            if (item.user().equals(from)) {
                return new CommentItem(item.id(), to, item.content(), item.time(), item.parentId(), item.upVotes(),
                        item.downVotes());
            }
            return item;
        }).collect(Collectors.toCollection(ArrayList::new)));
    }

    private static Path resolveStateFile() {
        String configured = System.getProperty("tihu.backend.state-file");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("TIHU_BACKEND_STATE_FILE");
        }
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim()).toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.home"), ".tihu-frontend", "backend-state.json").toAbsolutePath().normalize();
    }

    private boolean loadState() {
        if (!Files.exists(stateFile)) {
            return false;
        }
        try {
            PersistentState state = mapper.readValue(Files.readString(stateFile), PersistentState.class);
            applyState(state);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void persistState() {
        try {
            Path parent = stateFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tempDir = parent != null ? parent : stateFile.toAbsolutePath().getParent();
            Path temp = Files.createTempFile(tempDir, "tihu-state-", ".tmp");
            Files.writeString(temp, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot()));
            Files.move(temp, stateFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("无法保存后端数据", ex);
        }
    }

    private PersistentState snapshot() {
        return new PersistentState(
                bookIdSeq.get(),
                commentIdSeq.get(),
                listIdSeq.get(),
                users.values().stream()
                        .map(user -> new UserSnapshot(user.username, user.password, user.role,
                                user.bannedUntil == null ? null : user.bannedUntil.toString()))
                        .toList(),
                new ArrayList<>(books.values()),
                ratingMap,
                commentMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                                .map(item -> new CommentSnapshot(item.id(), item.user(), item.content(), item.time().toString(),
                                        item.parentId(), item.upVotes(), item.downVotes()))
                                .collect(Collectors.toCollection(ArrayList::new)), (left, right) -> left, LinkedHashMap::new)),
                voteMap,
                favoriteMap,
                userBookLists,
                followingMap,
                messageMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                                .map(item -> new MessageSnapshot(item.from(), item.to(), item.content(), item.time().toString()))
                                .collect(Collectors.toCollection(ArrayList::new)), (left, right) -> left, LinkedHashMap::new))
        );
    }

    private void applyState(PersistentState state) {
        users.clear();
        books.clear();
        ratingMap.clear();
        commentMap.clear();
        voteMap.clear();
        favoriteMap.clear();
        userBookLists.clear();
        followingMap.clear();
        messageMap.clear();

        if (state == null) {
            return;
        }

        bookIdSeq.set(state.bookIdSeq());
        commentIdSeq.set(state.commentIdSeq());
        listIdSeq.set(state.listIdSeq());

        if (state.users() != null) {
            for (UserSnapshot user : state.users()) {
                UserEntity entity = new UserEntity(user.username(), user.password(), user.role());
                entity.bannedUntil = parseDateTime(user.bannedUntil());
                users.put(user.username(), entity);
            }
        }

        if (state.books() != null) {
            for (Book book : state.books()) {
                books.put(book.id(), new Book(book.id(), book.title(), book.author(), book.intro(), new ArrayList<>(book.tags())));
            }
        }

        if (state.ratingMap() != null) {
            for (Map.Entry<Long, Map<String, Integer>> entry : state.ratingMap().entrySet()) {
                ratingMap.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }

        if (state.commentMap() != null) {
            for (Map.Entry<Long, List<CommentSnapshot>> entry : state.commentMap().entrySet()) {
                List<CommentItem> items = new ArrayList<>();
                for (CommentSnapshot snapshot : entry.getValue()) {
                    items.add(new CommentItem(snapshot.id(), snapshot.user(), snapshot.content(), parseDateTime(snapshot.time()),
                            snapshot.parentId(), snapshot.upVotes(), snapshot.downVotes()));
                }
                commentMap.put(entry.getKey(), items);
            }
        }

        if (state.voteMap() != null) {
            for (Map.Entry<Long, Map<String, Integer>> entry : state.voteMap().entrySet()) {
                voteMap.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }

        if (state.favoriteMap() != null) {
            for (Map.Entry<String, Set<Long>> entry : state.favoriteMap().entrySet()) {
                favoriteMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
        }

        if (state.userBookLists() != null) {
            for (Map.Entry<String, List<UserBookList>> entry : state.userBookLists().entrySet()) {
                List<UserBookList> lists = new ArrayList<>();
                for (UserBookList list : entry.getValue()) {
                    lists.add(new UserBookList(list.id(), list.owner(), list.title(), list.intro(), new ArrayList<>(list.bookIds())));
                }
                userBookLists.put(entry.getKey(), lists);
            }
        }

        if (state.followingMap() != null) {
            for (Map.Entry<String, Set<String>> entry : state.followingMap().entrySet()) {
                followingMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
        }

        if (state.messageMap() != null) {
            for (Map.Entry<String, List<MessageSnapshot>> entry : state.messageMap().entrySet()) {
                List<MessageItem> messages = new ArrayList<>();
                for (MessageSnapshot snapshot : entry.getValue()) {
                    messages.add(new MessageItem(snapshot.from(), snapshot.to(), snapshot.content(), parseDateTime(snapshot.time())));
                }
                messageMap.put(entry.getKey(), messages);
            }
        }

        for (String username : users.keySet()) {
            favoriteMap.putIfAbsent(username, new HashSet<>());
            userBookLists.putIfAbsent(username, new ArrayList<>());
            followingMap.putIfAbsent(username, new HashSet<>());
        }
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    private record PersistentState(int bookIdSeq, long commentIdSeq, long listIdSeq, List<UserSnapshot> users,
                                   List<Book> books, Map<Long, Map<String, Integer>> ratingMap,
                                   Map<Long, List<CommentSnapshot>> commentMap, Map<Long, Map<String, Integer>> voteMap,
                                   Map<String, Set<Long>> favoriteMap, Map<String, List<UserBookList>> userBookLists,
                                   Map<String, Set<String>> followingMap, Map<String, List<MessageSnapshot>> messageMap) {
    }

    private record UserSnapshot(String username, String password, Role role, String bannedUntil) {
    }

    private record CommentSnapshot(long id, String user, String content, String time, Long parentId, int upVotes,
                                   int downVotes) {
    }

    private record MessageSnapshot(String from, String to, String content, String time) {
    }

    private void seed() {
        users.put("admin", new UserEntity("admin", "Admin123", Role.ADMIN));
        users.put("alice", new UserEntity("alice", "Alice123", Role.USER));
        users.put("bob", new UserEntity("bob", "Bob12345", Role.USER));

        favoriteMap.put("admin", new HashSet<>());
        favoriteMap.put("alice", new HashSet<>());
        favoriteMap.put("bob", new HashSet<>());

        userBookLists.put("admin", new ArrayList<>());
        userBookLists.put("alice", new ArrayList<>());
        userBookLists.put("bob", new ArrayList<>());

        followingMap.put("admin", new HashSet<>());
        followingMap.put("alice", new HashSet<>());
        followingMap.put("bob", new HashSet<>());

        addSeedBook("三体", "刘慈欣", "地球文明与三体文明的接触与冲突。", "科幻 宇宙");
        addSeedBook("活着", "余华", "普通人在时代洪流中的生命故事。", "文学 现实");
        addSeedBook("解忧杂货店", "东野圭吾", "通过来信连接过去和现在的温暖故事。", "治愈 小说");
        addSeedBook("追风筝的人", "卡勒德胡赛尼", "关于成长、背叛与救赎。", "文学 成长");
        addSeedBook("白夜行", "东野圭吾", "跨越二十年的罪与罚。", "悬疑 小说");
        addSeedBook("嫌疑人X的献身", "东野圭吾", "天才数学家的极致爱情。", "悬疑 推理");
        addSeedBook("百年孤独", "加西亚马尔克斯", "布恩迪亚家族七代人的传奇。", "文学 魔幻现实");
        addSeedBook("人类简史", "尤瓦尔赫拉利", "从认知革命到现代社会。", "历史 社科");
        addSeedBook("原则", "瑞达利欧", "生活与工作的原则总结。", "商业 成长");
        addSeedBook("刻意练习", "安德斯艾利克森", "高水平表现背后的方法论。", "成长 方法");
        addSeedBook("被讨厌的勇气", "岸见一郎", "阿德勒心理学入门。", "心理 成长");

        rateBookInternal(101, "alice", 10);
        rateBookInternal(101, "bob", 9);
        rateBookInternal(102, "alice", 9);
        rateBookInternal(103, "bob", 8);

        addCommentInternal(101, "alice", "世界观很震撼。", null);
        CommentItem top = addCommentInternal(101, "bob", "前半部分慢热，后面很精彩。", null);
        addCommentInternal(101, "alice", "同感！", top.id());

        UserBookList list = createBookList("alice", "科幻入门", "适合刚接触科幻的读者");
        addBookToBookListInternal("alice", Math.toIntExact(list.id()), 101);
        addBookToBookListInternal("alice", Math.toIntExact(list.id()), 108);

        follow("alice", "bob");

        sendMessage("alice", "bob", "你好，我想交流下三体。\n");
        sendMessage("bob", "alice", "可以呀，约个时间聊。\n");
        persistState();
    }

    private void addSeedBook(String title, String author, String intro, String tagsText) {
        Book book = new Book(bookIdSeq.incrementAndGet(), title, author, intro, parseTags(tagsText));
        books.put(book.id(), book);
    }
}
