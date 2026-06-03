package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.service.MockBackendService.BookSortMode;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.BookDetailReturnTarget;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Arrays;
import java.util.List;

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
    @FXML private TextField searchTitleField;
    @FXML private TextField searchTagsField;
    @FXML private ComboBox<String> sortBox;
    @FXML private Label pageInfoLabel;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private static final int PAGE_SIZE = 6;
    private MainController mainController;
    private Long selectedBookId;
    private int currentPage = 1;
    private int totalPages = 1;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title()));
        authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().author()));
        ratingColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().averageScoreText()));
        tagsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().tagsSummary()));

        sortBox.getItems().addAll("默认排序", "按评分排序", "按书名排序");
        sortBox.getSelectionModel().select(0);
        sortBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            refresh();
        });

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> fillForm(newValue));
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenDetail();
            }
        });
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
            currentPage = 1;
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

    @FXML
    private void onSearch() {
        currentPage = 1;
        refresh();
    }

    @FXML
    private void onResetSearch() {
        searchTitleField.clear();
        searchTagsField.clear();
        currentPage = 1;
        sortBox.getSelectionModel().select(0);
        refresh();
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            refresh();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            refresh();
        }
    }

    @FXML
    private void onOpenDetail() {
        MockBackendService.BookCard selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null && mainController != null) {
            mainController.openBookDetail(selected.id(), BookDetailReturnTarget.ADMIN_BOOKS);
        }
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
        MockBackendService.BookListPage page = context.service().listBooks(searchTitleField.getText(), searchTags(), currentPage, PAGE_SIZE, currentSortMode());
        totalPages = page.totalPages();
        currentPage = Math.min(Math.max(1, page.page()), totalPages);
        tableView.getItems().setAll(page.items());
        pageInfoLabel.setText("第 " + currentPage + "/" + totalPages + " 页，共 " + page.totalItems() + " 本");
        messageLabel.setText("管理员可增删图书；V1封面统一默认图");
    }

    private List<String> searchTags() {
        return Arrays.stream(searchTagsField.getText() == null ? new String[0] : searchTagsField.getText().trim().split("\\s+"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    private BookSortMode currentSortMode() {
        int index = sortBox.getSelectionModel().getSelectedIndex();
        return switch (index) {
            case 1 -> BookSortMode.RATING_DESC;
            case 2 -> BookSortMode.TITLE_ASC;
            default -> BookSortMode.DEFAULT;
        };
    }
}

