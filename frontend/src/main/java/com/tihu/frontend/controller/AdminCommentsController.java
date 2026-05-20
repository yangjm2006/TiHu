package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class AdminCommentsController implements MainContentController {

    @FXML private ListView<String> commentsListView;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private List<MockBackendService.CommentItem> comments = List.of();

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void onDeleteSelected() {
        int idx = commentsListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= comments.size()) {
            messageLabel.setText("请先选择评论");
            return;
        }
        MockBackendService.CommentItem selected = comments.get(idx);
        context.service().adminDeleteComment(selected.id());
        refresh();
    }

    private void refresh() {
        comments = context.service().adminListAllComments();
        commentsListView.getItems().setAll(comments.stream()
                .map(item -> item.id() + " | " + item.user() + " | " + item.content())
                .toList());
        messageLabel.setText("管理员逻辑删除后前端不再显示评论");
    }
}

