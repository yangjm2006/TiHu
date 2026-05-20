package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class BookListsController implements MainContentController {

    @FXML private ListView<String> listView;
    @FXML private TextField titleField;
    @FXML private TextArea introArea;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void onCreate() {
        try {
            context.service().createBookList(context.username(), titleField.getText(), introArea.getText());
            titleField.clear();
            introArea.clear();
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onOpen() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            messageLabel.setText("请先选择书单");
            return;
        }
        MockBackendService.UserBookList selected = context.service().listBookLists(context.username()).get(idx);
        if (mainController != null) {
            mainController.openBookListDetail(selected.id());
        }
    }

    @FXML
    private void onDelete() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            messageLabel.setText("请先选择书单");
            return;
        }
        MockBackendService.UserBookList selected = context.service().listBookLists(context.username()).get(idx);
        context.service().deleteBookList(context.username(), selected.id());
        refresh();
    }

    private void refresh() {
        listView.getItems().setAll(context.service().listBookLists(context.username()).stream()
                .map(item -> item.title() + "（" + item.bookIds().size() + "本）")
                .toList());
        messageLabel.setText("书单全部公开，可被其他用户查看");
    }
}

