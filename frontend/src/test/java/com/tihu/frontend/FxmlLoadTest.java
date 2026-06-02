package com.tihu.frontend;

import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlLoadTest {
    @BeforeAll
    static void startJavaFxToolkit() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    void shouldLoadBookListAndFavoritesViews() throws Exception {
        assertNotNull(new FXMLLoader(MainApplication.class.getResource("book-list-view.fxml")).load());
        assertNotNull(new FXMLLoader(MainApplication.class.getResource("favorites-view.fxml")).load());
    }
}
