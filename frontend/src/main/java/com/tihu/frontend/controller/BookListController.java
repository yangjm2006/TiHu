package com.tihu.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BookListController {

    @FXML private ListView<String> bookListView;
    @FXML private Label bookTitleLabel;
    @FXML private Label bookInfoLabel;
    @FXML private TextArea bookDescArea;

    private final List<String> allBooks = List.of(
            "解忧杂货店",
            "三体",
            "活着",
            "追风筝的人"
    );

    private Consumer<String> onBookSelected;

    @FXML
    public void initialize() {
        bookListView.getItems().setAll(allBooks);
        bookListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                showBookDetail(newV);
                if (onBookSelected != null) {
                    onBookSelected.accept(newV);
                }
            }
        });
    }

    public void setOnBookSelected(Consumer<String> onBookSelected) {
        this.onBookSelected = onBookSelected;
    }

    public void filterBooks(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            bookListView.getItems().setAll(allBooks);
            return;
        }

        String lowerKeyword = keyword.trim().toLowerCase();
        bookListView.getItems().setAll(
                allBooks.stream()
                        .filter(book -> book.toLowerCase().contains(lowerKeyword))
                        .collect(Collectors.toList())
        );
    }

    private void showBookDetail(String title) {
        bookTitleLabel.setText(title);
        bookInfoLabel.setText("作者：待填  |  年份：待填  |  评分：待填");
        bookDescArea.setText("这里是《" + title + "》的简介预览。点击后会进入详情页。");
    }
}

