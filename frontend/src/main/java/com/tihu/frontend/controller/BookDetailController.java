package com.tihu.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class BookDetailController {

    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private TextArea descArea;
    @FXML private ListView<String> commentListView;
    @FXML private TextArea commentInputArea;

    private Runnable onBack;

    @FXML
    public void initialize() {
        showBook("三体");
    }

    public void setBook(String title) {
        showBook(title);
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    private void showBook(String title) {
        titleLabel.setText(title);

        if ("三体".equals(title)) {
            metaLabel.setText("作者：刘慈欣  |  年份：2008  |  评分：9.3  |  分类：科幻");
            descArea.setText("《三体》是刘慈欣创作的长篇科幻小说，讲述人类文明与三体文明之间的故事。\n\n这里先放占位简介，后续可以接数据库或接口数据。");
            commentListView.getItems().setAll(
                    "很震撼的一本书，世界观太宏大了。",
                    "前期略慢，但越看越上头。",
                    "科幻爱好者必读。"
            );
        } else if ("解忧杂货店".equals(title)) {
            metaLabel.setText("作者：东野圭吾  |  年份：2012  |  评分：8.6  |  分类：治愈/小说");
            descArea.setText("《解忧杂货店》讲述一家神秘杂货店通过信件连接过去与现在的故事。\n\n这里先放占位简介，后续可以接数据库或接口数据。");
            commentListView.getItems().setAll(
                    "读完很温暖。",
                    "故事结构很巧妙。"
            );
        } else if ("活着".equals(title)) {
            metaLabel.setText("作者：余华  |  年份：1993  |  评分：9.1  |  分类：现实主义");
            descArea.setText("《活着》是余华的代表作之一，讲述普通人在时代变迁中的命运。\n\n这里先放占位简介，后续可以接数据库或接口数据。");
            commentListView.getItems().setAll(
                    "文字朴素但很有力量。",
                    "看完心情复杂。"
            );
        } else {
            metaLabel.setText("作者：待填  |  年份：待填  |  评分：待填  |  分类：待填");
            descArea.setText("这里是《" + title + "》的详情介绍。后续可以接数据库或接口数据。");
            commentListView.getItems().setAll("暂无评论");
        }
    }

    @FXML
    private void onSubmitComment() {
        String text = commentInputArea.getText();
        if (text != null && !text.isBlank()) {
            commentListView.getItems().add(0, text.trim());
            commentInputArea.clear();
        }
    }

    @FXML
    private void onCollect() {
        metaLabel.setText(metaLabel.getText() + "  |  已收藏");
    }

    @FXML
    private void onBackToBooks() {
        if (onBack != null) {
            onBack.run();
        }
    }
}

