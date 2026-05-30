package com.tihu.frontend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockBackendServiceListSortingTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultSortShouldKeepInsertionOrder() {
        MockBackendService service = new MockBackendService(tempDir.resolve("state-default.json"));
        service.addBook("SortTest-1", "A", "", "");
        service.addBook("SortTest-2", "B", "", "");
        service.addBook("SortTest-3", "C", "", "");

        List<String> titles = service.listBooks("SortTest-", List.of(), 1, 10, MockBackendService.BookSortMode.DEFAULT)
                .items()
                .stream()
                .map(MockBackendService.BookCard::title)
                .toList();

        assertEquals(List.of("SortTest-1", "SortTest-2", "SortTest-3"), titles);
    }

    @Test
    void titleSortShouldSortAlphabetically() {
        MockBackendService service = new MockBackendService(tempDir.resolve("state-title.json"));
        service.addBook("SortTitle-Z", "A", "", "");
        service.addBook("SortTitle-A", "B", "", "");
        service.addBook("SortTitle-B", "C", "", "");

        List<String> titles = service.listBooks("SortTitle-", List.of(), 1, 10, MockBackendService.BookSortMode.TITLE_ASC)
                .items()
                .stream()
                .map(MockBackendService.BookCard::title)
                .toList();

        assertEquals(List.of("SortTitle-A", "SortTitle-B", "SortTitle-Z"), titles);
    }

    @Test
    void ratingSortShouldSortByAverageScoreDescending() {
        MockBackendService service = new MockBackendService(tempDir.resolve("state-rating.json"));
        service.registerUser("reader", "Reader123");
        MockBackendService.Book low = service.addBook("SortRate-Low", "A", "", "");
        MockBackendService.Book high = service.addBook("SortRate-High", "B", "", "");
        MockBackendService.Book mid = service.addBook("SortRate-Mid", "C", "", "");

        service.rateBook(low.id(), "reader", 5);
        service.rateBook(high.id(), "reader", 10);
        service.rateBook(mid.id(), "reader", 8);

        List<String> titles = service.listBooks("SortRate-", List.of(), 1, 10, MockBackendService.BookSortMode.RATING_DESC)
                .items()
                .stream()
                .map(MockBackendService.BookCard::title)
                .toList();

        assertEquals(List.of("SortRate-High", "SortRate-Mid", "SortRate-Low"), titles);
    }
}

