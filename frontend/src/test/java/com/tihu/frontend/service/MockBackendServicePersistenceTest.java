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
}

