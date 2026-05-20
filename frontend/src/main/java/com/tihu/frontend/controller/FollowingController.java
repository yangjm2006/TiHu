package com.tihu.frontend.controller;

import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class FollowingController implements MainContentController {

    @FXML private Label titleLabel;
    @FXML private ListView<String> listView;

    private final AppContext context = AppContext.getInstance();

    @Override
    public void onShow() {
        if (context.isShowingFollowers()) {
            titleLabel.setText("粉丝列表");
            listView.getItems().setAll(context.service().listFollowers(context.username()).stream().map(item -> item.username()).toList());
        } else {
            titleLabel.setText("关注列表");
            listView.getItems().setAll(context.service().listFollowing(context.username()).stream().map(item -> item.username()).toList());
        }
    }
}

