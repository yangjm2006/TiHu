package com.tihu.frontend.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private TextField searchField;
    @FXML private StackPane contentPane;

    private void showText(String text) {
        contentPane.getChildren().setAll(new Label(text));
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText();
        showText("搜索：" + (keyword.isBlank() ? "（空）" : keyword));
    }

    @FXML
    private void onHome() { showText("首页内容区域"); }

    @FXML
    private void onBooks() { showText("图书列表区域"); }

    @FXML
    private void onMovies() { showText("电影列表区域"); }

    @FXML
    private void onForum() { showText("讨论区区域"); }

    @FXML
    private void onProfile() { showText("个人中心区域"); }
}