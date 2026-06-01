package com.tihu.frontend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

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
}

