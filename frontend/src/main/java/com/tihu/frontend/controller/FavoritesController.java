package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.BookDetailReturnTarget;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class FavoritesController implements MainContentController {

    @FXML private TableView<MockBackendService.FavoriteItem> favoritesTable;
    @FXML private TableColumn<MockBackendService.FavoriteItem, String> titleColumn;
    @FXML private TableColumn<MockBackendService.FavoriteItem, String> authorColumn;
    @FXML private TableColumn<MockBackendService.FavoriteItem, String> ratingColumn;
    @FXML private TableColumn<MockBackendService.FavoriteItem, String> tagsColumn;
    @FXML private TableColumn<MockBackendService.FavoriteItem, String> collectedAtColumn;
    @FXML private TextField searchField;
    @FXML private Label pageInfoLabel;
    @FXML private Label messageLabel;

    private static final int PAGE_SIZE = 6;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private List<MockBackendService.FavoriteItem> allFavorites = List.of();
    private List<MockBackendService.FavoriteItem> filteredFavorites = List.of();
    private int currentPage = 1;
    private int totalPages = 1;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().book().title()));
        authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().book().author()));
        ratingColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().book().averageScoreText()));
        tagsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().book().tagsSummary()));
        collectedAtColumn.setCellValueFactory(data -> new SimpleStringProperty(formatTime(data.getValue().collectedAt())));

        favoritesTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenDetail();
            }
        });
    }

    @Override
    public void onShow() {
        loadFavorites();
    }

    @FXML
    private void onOpenDetail() {
        MockBackendService.FavoriteItem selected = favoritesTable.getSelectionModel().getSelectedItem();
        if (selected != null && mainController != null) {
            mainController.openBookDetail(selected.book().id(), BookDetailReturnTarget.FAVORITES);
        }
    }

    @FXML
    private void onSearch() {
        currentPage = 1;
        applyFilter();
    }

    @FXML
    private void onReset() {
        searchField.clear();
        currentPage = 1;
        applyFilter();
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            refreshPage();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            refreshPage();
        }
    }

    @FXML
    private void onUnfavoriteSelected() {
        MockBackendService.FavoriteItem selected = favoritesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("请先选择要取消收藏的图书");
            return;
        }
        try {
            context.service().toggleFavorite(context.username(), selected.book().id());
            loadFavorites();
            messageLabel.setText("已取消收藏：" + selected.book().title());
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void loadFavorites() {
        try {
            allFavorites = context.service().listFavoriteItems(context.username());
            applyFilter();
        } catch (Exception ex) {
            allFavorites = List.of();
            filteredFavorites = List.of();
            favoritesTable.getItems().clear();
            messageLabel.setText(ex.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filteredFavorites = allFavorites.stream()
                .filter(item -> keyword.isBlank()
                        || item.book().title().toLowerCase(Locale.ROOT).contains(keyword)
                        || item.book().tagsSummary().toLowerCase(Locale.ROOT).contains(keyword))
                .toList();
        refreshPage();
    }

    private void refreshPage() {
        int total = filteredFavorites.size();
        totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        currentPage = Math.min(Math.max(1, currentPage), totalPages);
        int from = Math.min((currentPage - 1) * PAGE_SIZE, total);
        int to = Math.min(from + PAGE_SIZE, total);
        favoritesTable.getItems().setAll(filteredFavorites.subList(from, to));
        pageInfoLabel.setText("第 " + currentPage + "/" + totalPages + " 页");
        messageLabel.setText("共 " + total + " 本收藏");
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "未知" : time.format(TIME_FORMATTER);
    }
}

