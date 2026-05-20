package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.time.format.DateTimeFormatter;

public class ChatController implements MainContentController {

    @FXML private Label titleLabel;
    @FXML private ListView<String> messageListView;
    @FXML private TextArea inputArea;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private String peer;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        peer = context.selectedConversationPeer();
        titleLabel.setText("与 " + peer + " 的私信");
        refresh();
    }

    @FXML
    private void onSend() {
        try {
            context.service().sendMessage(context.username(), peer, inputArea.getText());
            inputArea.clear();
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onBack() {
        if (mainController != null) {
            mainController.onConversations();
        }
    }

    private void refresh() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        messageListView.getItems().setAll(context.service().listMessages(context.username(), peer).stream()
                .map(item -> "[" + item.time().format(formatter) + "] " + item.from() + " -> " + item.to() + ": " + item.content())
                .toList());
    }
}

