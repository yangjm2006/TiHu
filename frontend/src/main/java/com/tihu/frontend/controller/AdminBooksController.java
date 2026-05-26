package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class AdminBooksController implements MainContentController {

    @FXML private TableView<MockBackendService.BookCard> tableView;
    @FXML private TableColumn<MockBackendService.BookCard, String> titleColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> authorColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> ratingColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> tagsColumn;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField tagsField;
    @FXML private TextArea introArea;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private Long selectedBookId;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title()));
        authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().author()));
        ratingColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().averageScoreText()));
        tagsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().tagsSummary()));

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> fillForm(newValue));
    }

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void onAddBook() {
        try {
            context.service().addBook(titleField.getText(), authorField.getText(), introArea.getText(), tagsField.getText());
            clearForm();
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onUpdateBook() {
        if (selectedBookId == null) {
            messageLabel.setText("请先选择要编辑的图书");
            return;
        }
        try {
            context.service().updateBook(selectedBookId, titleField.getText(), authorField.getText(), introArea.getText(), tagsField.getText());
            refresh();
            messageLabel.setText("图书已更新");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onDeleteBook() {
        MockBackendService.BookCard selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("请先选择图书");
            return;
        }
        context.service().deleteBook((long) selected.id());
        clearForm();
        refresh();
    }

    private void fillForm(MockBackendService.BookCard selected) {
        if (selected == null) {
            selectedBookId = null;
            return;
        }
        selectedBookId = selected.id();
        MockBackendService.Book book = context.service().getBook((long) selected.id());
        titleField.setText(book.title());
        authorField.setText(book.author());
        introArea.setText(book.intro());
        tagsField.setText(String.join(" ", book.tags()));
        messageLabel.setText("已载入选中图书，标签请用空格分隔后直接修改再更新");
    }

    private void clearForm() {
        selectedBookId = null;
        tableView.getSelectionModel().clearSelection();
        titleField.clear();
        authorField.clear();
        tagsField.clear();
        introArea.clear();
    }

    private void refresh() {
        tableView.getItems().setAll(context.service().listBooks("", java.util.List.of(), 1, 100).items());
        messageLabel.setText("管理员可增删图书；V1封面统一默认图");
    }
}

