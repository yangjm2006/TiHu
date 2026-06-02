package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminCommentsController implements MainContentController {

    @FXML private ListView<String> commentsListView;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private List<MockBackendService.CommentItem> comments = List.of();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        try {
            context.service().adminDeleteComment(selected.id());
            refresh();
            messageLabel.setText("评论已删除");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void refresh() {
        comments = context.service().adminListAllComments();
        commentsListView.getItems().setAll(comments.stream()
                .map(this::formatComment)
                .toList());
        messageLabel.setText("管理员逻辑删除后前端不再显示评论");
    }

    private String formatComment(MockBackendService.CommentItem item) {
        return bookText(item) + " | " + formatTime(item.time()) + " | " + relationText(item) + " | "
                + item.user() + "：" + item.content();
    }

    private String bookText(MockBackendService.CommentItem item) {
        if (item.bookTitle() != null && !item.bookTitle().isBlank()) {
            return "《" + item.bookTitle() + "》";
        }
        return item.bookId() == null ? "未知图书" : "图书ID " + item.bookId();
    }

    private String relationText(MockBackendService.CommentItem item) {
        return item.parentId() == null ? "一级评论" : "回复评论 " + item.parentId();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "未知时间" : time.format(TIME_FORMATTER);
    }
}

