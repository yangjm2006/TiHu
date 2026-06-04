package com.tihu.frontend.service;

import com.tihu.frontend.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MockBackendServicePersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void registeredUserShouldSurviveRestart() {
        Path stateFile = tempDir.resolve("backend-state.json");

        MockBackendService first = new MockBackendService(stateFile);
        first.registerUser("tom", "Tom123");

        MockBackendService restarted = new MockBackendService(stateFile);
        assertEquals(MockBackendService.Role.USER, restarted.login("tom", "Tom123"));
    }

    @Test
    void updatedPasswordShouldSurviveRestart() {
        Path stateFile = tempDir.resolve("backend-state.json");

        MockBackendService first = new MockBackendService(stateFile);
        first.registerUser("jane", "Jane123");
        first.updateProfile("jane", "jane", "Jane456");

        MockBackendService restarted = new MockBackendService(stateFile);
        assertThrows(IllegalArgumentException.class, () -> restarted.login("jane", "Jane123"));
        assertDoesNotThrow(() -> restarted.login("jane", "Jane456"));
    }

    @Test
    void deletingTopLevelCommentShouldRemoveItsReplies() {
        Path stateFile = tempDir.resolve("backend-state.json");
        MockBackendService service = new MockBackendService(stateFile);

        MockBackendService.BookDetail beforeDelete = service.getBookDetail(101, "bob");
        MockBackendService.CommentItem topLevel = beforeDelete.comments().stream()
                .filter(comment -> "bob".equals(comment.user()))
                .findFirst()
                .orElseThrow();

        service.deleteOwnComment(101, topLevel.id(), "bob");

        MockBackendService.BookDetail afterDelete = service.getBookDetail(101, "bob");
        assertEquals(0, afterDelete.comments().stream().filter(comment -> comment.id() == topLevel.id()).count());
        assertEquals(0, afterDelete.replies().stream().filter(reply -> topLevel.id() == reply.parentId()).count());
    }

    @Test
    void userCannotWithdrawAnotherUsersComment() {
        Path stateFile = tempDir.resolve("backend-state.json");
        MockBackendService service = new MockBackendService(stateFile);

        MockBackendService.CommentItem aliceComment = service.getBookDetail(101, "bob").comments().stream()
                .filter(comment -> "alice".equals(comment.user()))
                .findFirst()
                .orElseThrow();

        assertThrows(IllegalStateException.class,
                () -> service.deleteOwnComment(101, aliceComment.id(), "bob"));
    }

    @Test
    void privateBookListsShouldOnlyAppearOnOwnersProfile() {
        Path stateFile = tempDir.resolve("backend-state.json");
        MockBackendService service = new MockBackendService(stateFile);

        MockBackendService.UserBookList privateList =
                service.createBookList("bob", "私密书单", "仅自己可见", false);
        MockBackendService.UserBookList publicList =
                service.createBookList("bob", "公开书单", "所有人可见", true);

        MockBackendService.UserProfile viewedByAlice = service.getUserProfile("bob", "alice");
        MockBackendService.UserProfile viewedByBob = service.getUserProfile("bob", "bob");

        assertEquals(List.of(publicList.id()), viewedByAlice.bookLists().stream()
                .filter(list -> list.id() == privateList.id() || list.id() == publicList.id())
                .map(MockBackendService.UserBookList::id)
                .toList());
        assertEquals(2, viewedByBob.bookLists().stream()
                .filter(list -> list.id() == privateList.id() || list.id() == publicList.id())
                .count());

        service.updateBookListVisibility("bob", privateList.id(), true);
        assertEquals(2, service.getUserProfile("bob", "alice").bookLists().stream()
                .filter(list -> list.id() == privateList.id() || list.id() == publicList.id())
                .count());
    }

    @Test
    void bookCanBeAddedToBookListByTitle() {
        Path stateFile = tempDir.resolve("backend-state.json");
        MockBackendService service = new MockBackendService(stateFile);
        MockBackendService.UserBookList list = service.createBookList("bob", "待读", "", true);

        service.addBookToBookList("bob", list.id(), "三体");

        assertEquals(List.of(101L), service.getBookList("bob", list.id()).bookIds());
    }

    @Test
    void bookListShouldRequireExactBookTitleWhenAddingBook() {
        Path stateFile = tempDir.resolve("backend-state.json");
        MockBackendService service = new MockBackendService(stateFile);
        MockBackendService.UserBookList list = service.createBookList("bob", "待读", "", true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addBookToBookList("bob", list.id(), "三"));

        assertEquals("书名不存在", ex.getMessage());
        assertEquals(List.of(), service.getBookList("bob", list.id()).bookIds());
    }

    @Test
    void bannedUserShouldSeeUnbanTimeWhenLoggingIn() {
        Path stateFile = tempDir.resolve("backend-state.json");
        MockBackendService service = new MockBackendService(stateFile);
        LocalDateTime until = LocalDateTime.now().plusHours(2);

        service.banUser("bob", until);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.login("bob", "Bob12345"));
        assertEquals("您已被封禁，解封时间是 " + DateTimeUtil.format(until), ex.getMessage());
    }
}

