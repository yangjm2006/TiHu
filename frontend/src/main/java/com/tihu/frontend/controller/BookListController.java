package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.service.MockBackendService.BookSortMode;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Arrays;
import java.util.List;

public class BookListController implements MainContentController {

    @FXML private TextField titleSearchField;
    @FXML private TextField tagsSearchField;
    @FXML private ComboBox<String> sortBox;
    @FXML private TableView<MockBackendService.BookCard> bookTableView;
    @FXML private TableColumn<MockBackendService.BookCard, String> titleColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> authorColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> ratingColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> tagsColumn;
    @FXML private Label pageInfoLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean restoringState;

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
        sortBox.getSelectionModel().select(BookSortMode.DEFAULT == context.bookListSortMode() ? 0
                : context.bookListSortMode() == BookSortMode.RATING_DESC ? 1 : 2);
        sortBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {
            if (!restoringState) {
                refresh();
            }
        });

        bookTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenDetail();
            }
        });
        refresh();
    }

    @Override
    public void onShow() {
        restoringState = true;
        titleSearchField.setText(context.bookListTitleKeyword());
        tagsSearchField.setText(context.bookListTagsText());
        currentPage = context.bookListPage();
        selectSortBox(context.bookListSortMode());
        restoringState = false;
        refresh();
    }

    public void search(String keyword) {
        titleSearchField.setText(keyword == null ? "" : keyword.trim());
        currentPage = 1;
        refresh();
    }

    @FXML
    private void onSearch() {
        currentPage = 1;
        refresh();
    }

    @FXML
    private void onReset() {
        titleSearchField.clear();
        tagsSearchField.clear();
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
        MockBackendService.BookCard selected = bookTableView.getSelectionModel().getSelectedItem();
        if (selected != null && mainController != null) {
            persistState();
            mainController.openBookDetail(selected.id());
        }
    }

    private void refresh() {
        persistState();
        List<String> tags = Arrays.stream(tagsSearchField.getText() == null ? new String[0] : tagsSearchField.getText().trim().split("\\s+"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
        MockBackendService.BookListPage page = context.service().listBooks(titleSearchField.getText(), tags, currentPage, 10, currentSortMode());
        totalPages = page.totalPages();
        currentPage = Math.min(Math.max(1, page.page()), totalPages);
        bookTableView.getItems().setAll(page.items());
        pageInfoLabel.setText("第 " + currentPage + "/" + totalPages + " 页，共 " + page.totalItems() + " 本");
    }

    private void persistState() {
        context.setBookListTitleKeyword(titleSearchField.getText());
        context.setBookListTagsText(tagsSearchField.getText());
        context.setBookListPage(currentPage);
        context.setBookListSortMode(currentSortMode());
    }

    private BookSortMode currentSortMode() {
        int index = sortBox.getSelectionModel().getSelectedIndex();
        return switch (index) {
            case 1 -> BookSortMode.RATING_DESC;
            case 2 -> BookSortMode.TITLE_ASC;
            default -> BookSortMode.DEFAULT;
        };
    }

    private void selectSortBox(BookSortMode sortMode) {
        int index = switch (sortMode == null ? BookSortMode.DEFAULT : sortMode) {
            case RATING_DESC -> 1;
            case TITLE_ASC -> 2;
            case DEFAULT -> 0;
        };
        sortBox.getSelectionModel().select(index);
    }
}
