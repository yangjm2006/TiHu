package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.BookDetailReturnTarget;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

    @FXML private Button backButton;
    @FXML private Button favoriteButton;
    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private TextArea descArea;
    @FXML private Label ratingSummaryLabel;
    @FXML private Label favoriteCountLabel;
    @FXML private TextArea distributionArea;
    @FXML private Spinner<Integer> myScoreSpinner;
    @FXML private ComboBox<String> bookListComboBox;
    @FXML private ListView<String> commentListView;
    @FXML private TextArea commentInputArea;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private final List<MockBackendService.CommentItem> flattenComments = new ArrayList<>();
    private List<MockBackendService.UserBookList> myBookLists = List.of();
    private long bookId;

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
        Long selectedBookId = context.selectedBookId();
        if (selectedBookId != null) {
            bookId = selectedBookId;
            refresh();
            updateNavigationText();
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
            boolean wasCollected = context.service().isCollected(context.username(), bookId);
            context.service().toggleFavorite(context.username(), bookId);
            refresh();
            messageLabel.setText(wasCollected ? "取消收藏成功" : "收藏成功");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onSubmitComment() {
        try {
            String content = commentContent();
            if (content.isEmpty()) {
                messageLabel.setText("评论不能为空");
                return;
            }
            context.service().addComment(bookId, context.username(), content, null);
            commentInputArea.clear();
            refresh();
            messageLabel.setText("评论已发布");
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
            String content = commentContent();
            if (content.isEmpty()) {
                messageLabel.setText("回复不能为空");
                return;
            }
            context.service().addComment(bookId, context.username(), content, selected.id());
            commentInputArea.clear();
            refresh();
            messageLabel.setText("回复已发布");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onDeleteOwnComment() {
        try {
            int idx = commentListView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= flattenComments.size()) {
                messageLabel.setText("请先选择评论");
                return;
            }
            MockBackendService.CommentItem selected = flattenComments.get(idx);
            if (context.isAdmin()) {
                context.service().adminDeleteComment(selected.id());
                refresh();
                messageLabel.setText("评论已删除");
                return;
            }
            if (!Objects.equals(selected.user(), context.username())) {
                messageLabel.setText("只能撤回自己的评论");
                return;
            }
            context.service().deleteOwnComment(bookId, selected.id(), context.username());
            refresh();
            messageLabel.setText("评论已撤回");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onAddToBookList() {
        try {
            int idx = bookListComboBox == null ? -1 : bookListComboBox.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= myBookLists.size()) {
                messageLabel.setText("请先选择书单");
                return;
            }
            MockBackendService.UserBookList selected = myBookLists.get(idx);
            context.service().addBookToBookList(context.username(), selected.id(), bookId);
            refresh();
            messageLabel.setText("已加入书单：" + selected.title());
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
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
            if (context.bookDetailReturnTarget() == BookDetailReturnTarget.FAVORITES) {
                mainController.onFavorites();
            } else if (context.bookDetailReturnTarget() == BookDetailReturnTarget.BOOK_LIST_DETAIL
                    && context.selectedBookListId() != null) {
                String owner = context.selectedBookListOwner() == null ? context.username() : context.selectedBookListOwner();
                mainController.openBookListDetail(owner, context.selectedBookListId(), context.bookListDetailReturnTarget());
            } else {
                mainController.onBooks();
            }
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
            messageLabel.setText(target == 1 ? "点赞已更新" : "点踩已更新");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void refresh() {
        MockBackendService.BookDetail detail = context.service().getBookDetail(bookId, context.username());
        titleLabel.setText(detail.book().title());
        metaLabel.setText("作者：" + detail.book().author() + "  |  标签：" + String.join(" ", detail.book().tags()));
        descArea.setText(detail.book().intro());

        MockBackendService.RatingSummary summary = detail.ratingSummary();
        String avgText = summary.count() == 0 ? "暂无评分" : String.format("%.1f", summary.average());
        ratingSummaryLabel.setText("平均分：" + avgText + "（" + summary.count() + "人）");
        if (favoriteCountLabel != null) {
            favoriteCountLabel.setText("收藏人数：" + detail.favoriteCount() + "人");
        }
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
        updateFavoriteButton();
        updateBookListOptions();
    }

    private void updateNavigationText() {
        if (backButton != null) {
            if (context.bookDetailReturnTarget() == BookDetailReturnTarget.FAVORITES) {
                backButton.setText("← 返回收藏");
            } else if (context.bookDetailReturnTarget() == BookDetailReturnTarget.BOOK_LIST_DETAIL) {
                backButton.setText("← 返回书单");
            } else {
                backButton.setText("← 返回列表");
            }
        }
    }

    private void updateFavoriteButton() {
        if (favoriteButton != null) {
            boolean collected = context.service().isCollected(context.username(), bookId);
            favoriteButton.setText(collected ? "取消收藏" : "收藏");
        }
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

    private void updateBookListOptions() {
        if (bookListComboBox == null) {
            return;
        }
        int selectedIndex = bookListComboBox.getSelectionModel().getSelectedIndex();
        myBookLists = context.service().listBookLists(context.username());
        bookListComboBox.getItems().setAll(myBookLists.stream()
                .map(list -> list.title() + "（" + (list.publicVisible() ? "公开" : "私密") + "）")
                .toList());
        if (!myBookLists.isEmpty()) {
            bookListComboBox.getSelectionModel().select(Math.min(Math.max(selectedIndex, 0), myBookLists.size() - 1));
        }
    }

    private String commentContent() {
        String content = commentInputArea.getText();
        return content == null ? "" : content.trim();
    }
}
