package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;

import java.util.List;

public class BookListsController implements MainContentController {

    @FXML private ListView<String> listView;
    @FXML private TextField titleField;
    @FXML private TextArea introArea;
    @FXML private CheckBox publicCheckBox;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private List<MockBackendService.UserBookList> bookLists = List.of();

    @FXML
    public void initialize() {
        listView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpen();
            }
        });
    }

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
            context.service().createBookList(context.username(), titleField.getText(), introArea.getText(),
                    publicCheckBox == null || publicCheckBox.isSelected());
            titleField.clear();
            introArea.clear();
            if (publicCheckBox != null) {
                publicCheckBox.setSelected(true);
            }
            refresh();
            messageLabel.setText("书单已创建");
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
        MockBackendService.UserBookList selected = bookLists.get(idx);
        if (mainController != null) {
            mainController.openBookListDetail(selected.id());
        }
    }

    @FXML
    private void onToggleVisibility() {
        try {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= bookLists.size()) {
                messageLabel.setText("请先选择书单");
                return;
            }
            MockBackendService.UserBookList selected = bookLists.get(idx);
            boolean nextVisible = !selected.publicVisible();
            context.service().updateBookListVisibility(context.username(), selected.id(), nextVisible);
            refresh();
            messageLabel.setText(nextVisible ? "已切换为公开书单" : "已切换为私密书单");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            messageLabel.setText("请先选择书单");
            return;
        }
        MockBackendService.UserBookList selected = bookLists.get(idx);
        context.service().deleteBookList(context.username(), selected.id());
        refresh();
    }

    private void refresh() {
        bookLists = context.service().listBookLists(context.username());
        listView.getItems().setAll(bookLists.stream()
                .map(item -> item.title() + "（" + item.bookIds().size() + "本，" + visibilityText(item) + "）")
                .toList());
        messageLabel.setText("公开书单所有人可见，私密书单仅自己可见");
    }

    private String visibilityText(MockBackendService.UserBookList list) {
        return list.publicVisible() ? "公开" : "私密";
    }
}

