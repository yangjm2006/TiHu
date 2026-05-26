package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.BookDetailReturnTarget;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class FavoritesController implements MainContentController {

    @FXML private TableView<MockBackendService.BookCard> favoritesTable;
    @FXML private TableColumn<MockBackendService.BookCard, String> titleColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> authorColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> ratingColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> tagsColumn;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;

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

        favoritesTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenDetail();
            }
        });
    }

    @Override
    public void onShow() {
        favoritesTable.getItems().setAll(context.service().listFavorites(context.username()));
        messageLabel.setText("共 " + favoritesTable.getItems().size() + " 本");
    }

    @FXML
    private void onOpenDetail() {
        MockBackendService.BookCard selected = favoritesTable.getSelectionModel().getSelectedItem();
        if (selected != null && mainController != null) {
            mainController.openBookDetail(selected.id(), BookDetailReturnTarget.FAVORITES);
        }
    }
}

