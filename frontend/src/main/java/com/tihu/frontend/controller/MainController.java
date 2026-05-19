package com.tihu.frontend.controller;

import com.tihu.frontend.MainApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private TextField searchField;
    @FXML private StackPane contentPane;

    private BookListController bookListController;

    @FXML
    public void initialize() {
        showHome();
    }

    private void setContent(Node view) {
        contentPane.getChildren().setAll(view);
    }

    private void showBooks() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("book-list-view.fxml"));
            setContent(loader.load());
            bookListController = loader.getController();
            bookListController.setOnBookSelected(this::showBookDetail);
        } catch (Exception e) {
            setContent(new Label("加载图书列表失败：" + e.getMessage()));
        }
    }

    private void showBookDetail(String title) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("book-detail-view.fxml"));
            setContent(loader.load());
            BookDetailController controller = loader.getController();
            controller.setBook(title);
            controller.setOnBack(this::showBooks);
        } catch (Exception e) {
            setContent(new Label("加载图书详情失败：" + e.getMessage()));
        }
    }

    @FXML private void onSearch() {
        String keyword = searchField.getText();
        showBooks();
        bookListController.filterBooks(keyword);
    }

    @FXML private void onHome() { showHome(); }
    @FXML private void onBooks() { showBooks(); }
    @FXML private void onMovies() { showPlaceholder("电影模块"); }
    @FXML private void onForum() { showPlaceholder("讨论区模块"); }
    @FXML private void onProfile() { showPlaceholder("个人中心模块"); }

    private void showHome() {
        showPlaceholder("欢迎来到 TiHu", "这里是首页，后续可展示热门图书、最新评论和推荐内容。");
    }

    private void showPlaceholder(String title) {
        showPlaceholder(title, "该模块后续接入更多功能。");
    }

    private void showPlaceholder(String title, String message) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: #555;");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12, titleLabel, messageLabel);
        box.setStyle("-fx-padding: 24;");
        setContent(box);
    }
}