package com.tihu.frontend.service;

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

    public enum Role {
        USER,
        ADMIN
    }

    public record Book(int id, String title, String author, String intro, List<String> tags) {
    }

    public record BookListPage(List<BookCard> items, int page, int totalPages, int totalItems) {
    }

    public record BookCard(int id, String title, String author, String tagsSummary, String averageScoreText) {
    }

    public record RatingSummary(double average, int count, Map<Integer, Integer> distribution, Integer myScore) {
    }

    public record CommentItem(long id, String user, String content, LocalDateTime time, Long parentId, int upVotes, int downVotes) {
    }

    public record BookDetail(Book book, RatingSummary ratingSummary, List<CommentItem> comments, List<CommentItem> replies) {
    }

    public record UserBookList(long id, String owner, String title, String intro, List<Integer> bookIds) {
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

    private final Map<String, UserEntity> users = new LinkedHashMap<>();
    private final Map<Integer, Book> books = new LinkedHashMap<>();
    private final Map<Integer, Map<String, Integer>> ratingMap = new HashMap<>();
    private final Map<Integer, List<CommentItem>> commentMap = new HashMap<>();
    private final Map<Long, Map<String, Integer>> voteMap = new HashMap<>();
    private final Map<String, Set<Integer>> favoriteMap = new HashMap<>();
    private final Map<String, List<UserBookList>> userBookLists = new HashMap<>();
    private final Map<String, Set<String>> followingMap = new HashMap<>();
    private final Map<String, List<MessageItem>> messageMap = new HashMap<>();

    public MockBackendService() {
        seed();
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
    }

    public synchronized BookListPage listBooks(String titleKeyword, List<String> tags, int page, int pageSize) {
        List<Book> filtered = books.values().stream()
                .filter(book -> containsKeyword(book.title(), titleKeyword))
                .filter(book -> containsAllTags(book.tags(), tags))
                .sorted(Comparator.comparingDouble((Book b) -> averageScore(b.id())).reversed().thenComparing(Book::title))
                .toList();

        int safePage = Math.max(1, page);
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int from = Math.min((safePage - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);

        List<BookCard> items = filtered.subList(from, to).stream().map(this::toCard).toList();
        return new BookListPage(items, safePage, totalPages, total);
    }

    public synchronized BookDetail getBookDetail(int bookId, String currentUser) {
        Book book = getBook(bookId);
        RatingSummary summary = ratingSummary(bookId, currentUser);

        List<CommentItem> allComments = commentMap.getOrDefault(bookId, List.of()).stream()
                .sorted(Comparator.comparing(CommentItem::time).reversed())
                .toList();

        List<CommentItem> comments = allComments.stream().filter(item -> item.parentId() == null).toList();
        List<CommentItem> replies = allComments.stream().filter(item -> item.parentId() != null).toList();
        return new BookDetail(book, summary, comments, replies);
    }

    public synchronized void rateBook(int bookId, String username, int score) {
        if (score < 1 || score > 10) {
            throw new IllegalArgumentException("评分必须在 1~10");
        }
        getRequiredUser(username);
        getBook(bookId);
        ratingMap.computeIfAbsent(bookId, key -> new HashMap<>()).put(username, score);
    }

    public synchronized void toggleFavorite(String username, int bookId) {
        getRequiredUser(username);
        getBook(bookId);
        Set<Integer> set = favoriteMap.computeIfAbsent(username, key -> new HashSet<>());
        if (!set.add(bookId)) {
            set.remove(bookId);
        }
    }

    public synchronized List<BookCard> listFavorites(String username) {
        getRequiredUser(username);
        return favoriteMap.getOrDefault(username, Set.of()).stream()
                .map(this::getBook)
                .sorted(Comparator.comparing(Book::title))
                .map(this::toCard)
                .toList();
    }

    public synchronized CommentItem addComment(int bookId, String username, String content, Long parentCommentId) {
        getRequiredUser(username);
        getBook(bookId);
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

    public synchronized void updateBook(int bookId, String title, String author, String intro, String tagsText) {
        Book old = getBook(bookId);
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

    public synchronized void deleteOwnComment(int bookId, long commentId, String username) {
        List<CommentItem> list = commentMap.getOrDefault(bookId, new ArrayList<>());
        list.removeIf(item -> item.id() == commentId && item.user().equals(username));
    }

    public synchronized void adminDeleteComment(int bookId, long commentId) {
        List<CommentItem> list = commentMap.getOrDefault(bookId, new ArrayList<>());
        list.removeIf(item -> item.id() == commentId);
    }

    public synchronized void adminDeleteComment(long commentId) {
        for (List<CommentItem> list : commentMap.values()) {
            if (list.removeIf(item -> item.id() == commentId)) {
                return;
            }
        }
    }

    public synchronized void voteComment(long commentId, String username, int target) {
        if (target != -1 && target != 0 && target != 1) {
            throw new IllegalArgumentException("投票参数错误");
        }
        getRequiredUser(username);

        CommentItem hit = null;
        int hitBookId = -1;
        for (Map.Entry<Integer, List<CommentItem>> entry : commentMap.entrySet()) {
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
        return list;
    }

    public synchronized void deleteBookList(String username, long listId) {
        userBookLists.getOrDefault(username, new ArrayList<>()).removeIf(item -> item.id() == listId);
    }

    public synchronized UserBookList getBookList(String owner, long listId) {
        return userBookLists.getOrDefault(owner, List.of()).stream()
                .filter(item -> item.id() == listId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("书单不存在"));
    }

    public synchronized void addBookToBookList(String username, long listId, int bookId) {
        UserBookList item = getBookList(username, listId);
        if (item.bookIds().contains(bookId)) {
            throw new IllegalStateException("同一本书不能重复加入书单");
        }
        ((ArrayList<Integer>) item.bookIds()).add(bookId);
    }

    public synchronized void removeBookFromBookList(String username, long listId, int bookId) {
        UserBookList item = getBookList(username, listId);
        item.bookIds().remove(Integer.valueOf(bookId));
    }

    public synchronized void follow(String me, String target) {
        if (Objects.equals(me, target)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        getRequiredUser(me);
        getRequiredUser(target);
        followingMap.computeIfAbsent(me, key -> new HashSet<>()).add(target);
    }

    public synchronized void unfollow(String me, String target) {
        followingMap.computeIfAbsent(me, key -> new HashSet<>()).remove(target);
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
    }

    public synchronized void unbanUser(String username) {
        UserEntity user = getRequiredUser(username);
        user.bannedUntil = null;
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
        return book;
    }

    public synchronized void deleteBook(int bookId) {
        books.remove(bookId);
        ratingMap.remove(bookId);
        commentMap.remove(bookId);
        favoriteMap.values().forEach(set -> set.remove(bookId));
        userBookLists.values().forEach(lists -> lists.forEach(list -> list.bookIds().remove(Integer.valueOf(bookId))));
    }

    private BookCard toCard(Book book) {
        double avg = averageScore(book.id());
        String avgText = avg > 0 ? String.format(Locale.US, "%.1f", avg) : "暂无评分";
        String tags = String.join(", ", book.tags());
        return new BookCard(book.id(), book.title(), book.author(), tags, avgText);
    }

    private RatingSummary ratingSummary(int bookId, String username) {
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

    private boolean isTopLevelComment(int bookId, long commentId) {
        return commentMap.getOrDefault(bookId, List.of()).stream()
                .anyMatch(item -> item.id() == commentId && item.parentId() == null);
    }

    private double averageScore(int bookId) {
        Map<String, Integer> map = ratingMap.getOrDefault(bookId, Map.of());
        if (map.isEmpty()) {
            return 0;
        }
        return map.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public synchronized Book getBook(int id) {
        Book book = books.get(id);
        if (book == null) {
            throw new IllegalArgumentException("图书不存在");
        }
        return book;
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
        return List.of(tagsText.split(",")).stream()
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

        addSeedBook("三体", "刘慈欣", "地球文明与三体文明的接触与冲突。", "科幻, 宇宙");
        addSeedBook("活着", "余华", "普通人在时代洪流中的生命故事。", "文学, 现实");
        addSeedBook("解忧杂货店", "东野圭吾", "通过来信连接过去和现在的温暖故事。", "治愈, 小说");
        addSeedBook("追风筝的人", "卡勒德胡赛尼", "关于成长、背叛与救赎。", "文学, 成长");
        addSeedBook("白夜行", "东野圭吾", "跨越二十年的罪与罚。", "悬疑, 小说");
        addSeedBook("嫌疑人X的献身", "东野圭吾", "天才数学家的极致爱情。", "悬疑, 推理");
        addSeedBook("百年孤独", "加西亚马尔克斯", "布恩迪亚家族七代人的传奇。", "文学, 魔幻现实");
        addSeedBook("人类简史", "尤瓦尔赫拉利", "从认知革命到现代社会。", "历史, 社科");
        addSeedBook("原则", "瑞达利欧", "生活与工作的原则总结。", "商业, 成长");
        addSeedBook("刻意练习", "安德斯艾利克森", "高水平表现背后的方法论。", "成长, 方法");
        addSeedBook("被讨厌的勇气", "岸见一郎", "阿德勒心理学入门。", "心理, 成长");

        rateBook(101, "alice", 10);
        rateBook(101, "bob", 9);
        rateBook(102, "alice", 9);
        rateBook(103, "bob", 8);

        addComment(101, "alice", "世界观很震撼。", null);
        CommentItem top = addComment(101, "bob", "前半部分慢热，后面很精彩。", null);
        addComment(101, "alice", "同感！", top.id());

        UserBookList list = createBookList("alice", "科幻入门", "适合刚接触科幻的读者");
        addBookToBookList("alice", list.id(), 101);
        addBookToBookList("alice", list.id(), 108);

        follow("alice", "bob");

        sendMessage("alice", "bob", "你好，我想交流下三体。\n");
        sendMessage("bob", "alice", "可以呀，约个时间聊。\n");
    }

    private void addSeedBook(String title, String author, String intro, String tagsText) {
        Book book = new Book(bookIdSeq.incrementAndGet(), title, author, intro, parseTags(tagsText));
        books.put(book.id(), book);
    }
}

