package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BookDetailController implements MainContentController {

    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private TextArea descArea;
    @FXML private Label ratingSummaryLabel;
    @FXML private TextArea distributionArea;
    @FXML private Spinner<Integer> myScoreSpinner;
    @FXML private ListView<String> commentListView;
    @FXML private TextArea commentInputArea;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private final List<MockBackendService.CommentItem> flattenComments = new ArrayList<>();
    private int bookId;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        myScoreSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
    }

    @Override
    public void onShow() {
        Integer selectedBookId = context.selectedBookId();
        if (selectedBookId != null) {
            bookId = selectedBookId;
            refresh();
        }
    }

    @FXML
    private void onRate() {
        try {
            context.service().rateBook(bookId, context.username(), myScoreSpinner.getValue());
            messageLabel.setText("评分已提交");
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onCollect() {
        try {
            context.service().toggleFavorite(context.username(), bookId);
            messageLabel.setText("收藏状态已切换");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onSubmitComment() {
        try {
            context.service().addComment(bookId, context.username(), commentInputArea.getText(), null);
            commentInputArea.clear();
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onReplySelected() {
        try {
            int idx = commentListView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= flattenComments.size()) {
                messageLabel.setText("请先选择一级评论");
                return;
            }
            MockBackendService.CommentItem selected = flattenComments.get(idx);
            if (selected.parentId() != null) {
                messageLabel.setText("只能回复一级评论");
                return;
            }
            context.service().addComment(bookId, context.username(), commentInputArea.getText(), selected.id());
            commentInputArea.clear();
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onDeleteOwnComment() {
        int idx = commentListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= flattenComments.size()) {
            messageLabel.setText("请先选择评论");
            return;
        }
        MockBackendService.CommentItem selected = flattenComments.get(idx);
        if (context.isAdmin()) {
            context.service().adminDeleteComment(bookId, selected.id());
        } else {
            context.service().deleteOwnComment(bookId, selected.id(), context.username());
        }
        refresh();
    }

    @FXML
    private void onUpVote() {
        voteSelected(1);
    }

    @FXML
    private void onDownVote() {
        voteSelected(-1);
    }

    @FXML
    private void onBackToBooks() {
        if (mainController != null) {
            mainController.onBooks();
        }
    }

    private void voteSelected(int target) {
        int idx = commentListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= flattenComments.size()) {
            messageLabel.setText("请先选择评论");
            return;
        }
        try {
            context.service().voteComment(flattenComments.get(idx).id(), context.username(), target);
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void refresh() {
        MockBackendService.BookDetail detail = context.service().getBookDetail(bookId, context.username());
        titleLabel.setText(detail.book().title());
        metaLabel.setText("作者：" + detail.book().author() + "  |  标签：" + String.join(", ", detail.book().tags()));
        descArea.setText(detail.book().intro());

        MockBackendService.RatingSummary summary = detail.ratingSummary();
        String avgText = summary.count() == 0 ? "暂无评分" : String.format("%.1f", summary.average());
        ratingSummaryLabel.setText("平均分：" + avgText + "（" + summary.count() + "人）");
        distributionArea.setText(formatDistribution(summary.distribution()));
        if (summary.myScore() != null) {
            myScoreSpinner.getValueFactory().setValue(summary.myScore());
        }

        flattenComments.clear();
        List<String> textItems = new ArrayList<>();
        for (MockBackendService.CommentItem comment : detail.comments()) {
            flattenComments.add(comment);
            textItems.add(formatComment(comment, false));
            for (MockBackendService.CommentItem reply : detail.replies()) {
                if (Objects.equals(reply.parentId(), comment.id())) {
                    flattenComments.add(reply);
                    textItems.add(formatComment(reply, true));
                }
            }
        }
        commentListView.getItems().setAll(textItems);
    }

    private String formatDistribution(Map<Integer, Integer> dist) {
        StringBuilder sb = new StringBuilder();
        for (int i = 10; i >= 1; i--) {
            sb.append(i).append("分：").append(dist.getOrDefault(i, 0)).append("人");
            if (i > 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String formatComment(MockBackendService.CommentItem item, boolean reply) {
        String prefix = reply ? "  -> " : "";
        return prefix + item.user() + "：" + item.content() + "  [赞" + item.upVotes() + "/踩" + item.downVotes() + "]";
    }
}
