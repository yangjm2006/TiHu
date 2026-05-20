package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
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

        sortBox.getItems().addAll("评分从高到低");
        sortBox.getSelectionModel().selectFirst();

        bookTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenDetail();
            }
        });
        refresh();
    }

    @Override
    public void onShow() {
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
            mainController.openBookDetail(selected.id());
        }
    }

    private void refresh() {
        List<String> tags = Arrays.stream(tagsSearchField.getText() == null ? new String[0] : tagsSearchField.getText().split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
        MockBackendService.BookListPage page = context.service().listBooks(titleSearchField.getText(), tags, currentPage, 10);
        totalPages = page.totalPages();
        currentPage = Math.min(Math.max(1, page.page()), totalPages);
        bookTableView.getItems().setAll(page.items());
        pageInfoLabel.setText("第 " + currentPage + "/" + totalPages + " 页，共 " + page.totalItems() + " 本");
    }
}
