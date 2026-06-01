package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.BookDetailReturnTarget;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;

import java.util.List;

public class BookListDetailController implements MainContentController {

    @FXML private Label ownerLabel;
    @FXML private Label titleLabel;
    @FXML private Label introLabel;
    @FXML private Label visibilityLabel;
    @FXML private ListView<String> booksListView;
    @FXML private TextField bookTitleField;
    @FXML private Button addBookButton;
    @FXML private Button removeBookButton;
    @FXML private Button toggleVisibilityButton;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private MockBackendService.UserBookList list;
    private String owner;

    @FXML
    public void initialize() {
        booksListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpenSelectedBook();
            }
        });
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        Long listId = context.selectedBookListId();
        if (listId != null) {
            owner = context.selectedBookListOwner() == null ? context.username() : context.selectedBookListOwner();
            list = context.service().getBookList(owner, listId);
            refresh();
        }
    }

    @FXML
    private void onAddBook() {
        try {
            if (!isOwnerEditable()) {
                messageLabel.setText("当前仅可查看他人公开书单，不能修改");
                return;
            }
            String bookTitle = bookTitleField.getText() == null ? "" : bookTitleField.getText().trim();
            if (bookTitle.isBlank()) {
                messageLabel.setText("请输入图书名称");
                return;
            }
            context.service().addBookToBookList(context.username(), list.id(), bookTitle);
            bookTitleField.clear();
            refresh();
            messageLabel.setText("图书已加入书单");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onRemoveBook() {
        try {
            if (!isOwnerEditable()) {
                messageLabel.setText("当前仅可查看他人公开书单，不能修改");
                return;
            }
            int idx = booksListView.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                messageLabel.setText("请先选择图书");
                return;
            }
            List<Long> ids = list.bookIds();
            long bookId = ids.get(idx);
            context.service().removeBookFromBookList(context.username(), list.id(), bookId);
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onToggleVisibility() {
        try {
            if (!isOwnerEditable()) {
                messageLabel.setText("当前仅可查看他人公开书单，不能修改");
                return;
            }
            boolean nextVisible = !list.publicVisible();
            context.service().updateBookListVisibility(context.username(), list.id(), nextVisible);
            refresh();
            messageLabel.setText(nextVisible ? "已切换为公开书单" : "已切换为私密书单");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onOpenSelectedBook() {
        int idx = booksListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || list == null || idx >= list.bookIds().size()) {
            messageLabel.setText("请先选择图书");
            return;
        }
        if (mainController != null) {
            mainController.openBookDetail(list.bookIds().get(idx), BookDetailReturnTarget.BOOK_LIST_DETAIL);
        }
    }

    @FXML
    private void onBack() {
        if (mainController != null) {
            if (owner != null && !owner.equals(context.username())) {
                mainController.openUserProfile(owner);
            } else {
                mainController.onBookLists();
            }
        }
    }

    private void refresh() {
        list = context.service().getBookList(owner, list.id());
        ownerLabel.setText("书单作者：" + list.owner());
        titleLabel.setText(list.title());
        introLabel.setText(list.intro());
        if (visibilityLabel != null) {
            visibilityLabel.setText("可见性：" + (list.publicVisible() ? "公开" : "私密"));
        }
        booksListView.getItems().setAll(list.bookIds().stream()
                .map(id -> context.service().getBook(id).title())
                .toList());
        updateEditControls();
        messageLabel.setText(isOwnerEditable() ? "输入图书名称即可加入书单" : "当前为公开书单，仅可查看");
    }

    private boolean isOwnerEditable() {
        return list != null && context.username().equals(list.owner());
    }

    private void updateEditControls() {
        boolean editable = isOwnerEditable();
        if (bookTitleField != null) {
            bookTitleField.setDisable(!editable);
        }
        if (addBookButton != null) {
            addBookButton.setDisable(!editable);
        }
        if (removeBookButton != null) {
            removeBookButton.setDisable(!editable);
        }
        if (toggleVisibilityButton != null) {
            toggleVisibilityButton.setDisable(!editable);
        }
    }
}

