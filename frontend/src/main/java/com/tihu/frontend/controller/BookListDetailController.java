package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.List;

public class BookListDetailController implements MainContentController {

    @FXML private Label ownerLabel;
    @FXML private Label titleLabel;
    @FXML private Label introLabel;
    @FXML private ListView<String> booksListView;
    @FXML private TextField bookIdField;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private MockBackendService.UserBookList list;
    private String owner;

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
            int bookId = Integer.parseInt(bookIdField.getText().trim());
            context.service().addBookToBookList(context.username(), list.id(), bookId);
            bookIdField.clear();
            refresh();
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
            List<Integer> ids = list.bookIds();
            int bookId = ids.get(idx);
            context.service().removeBookFromBookList(context.username(), list.id(), bookId);
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onBack() {
        if (mainController != null) {
            mainController.onBookLists();
        }
    }

    private void refresh() {
        list = context.service().getBookList(owner, list.id());
        ownerLabel.setText("书单作者：" + list.owner());
        titleLabel.setText(list.title());
        introLabel.setText(list.intro());
        booksListView.getItems().setAll(list.bookIds().stream()
                .map(id -> "图书ID: " + id + " | " + context.service().getBook(id).title())
                .toList());
        messageLabel.setText(isOwnerEditable() ? "同一本书在同一书单中只能出现一次" : "当前为公开书单，仅可查看");
    }

    private boolean isOwnerEditable() {
        return list != null && context.username().equals(list.owner());
    }
}

