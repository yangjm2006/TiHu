package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConversationsController implements MainContentController {

    @FXML private ListView<String> conversationListView;
    @FXML private TextField peerField;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private List<MockBackendService.ConversationPreview> conversations = List.of();

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        conversations = context.service().listConversations(context.username());
        conversationListView.getItems().setAll(conversations.stream()
                .map(item -> item.peer() + " | " + item.lastTime().format(formatter) + " | " + item.lastMessage())
                .toList());
    }

    @FXML
    private void onOpenChat() {
        int idx = conversationListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= conversations.size()) {
            messageLabel.setText("请先选择会话");
            return;
        }
        if (mainController != null) {
            mainController.openChat(conversations.get(idx).peer());
        }
    }

    @FXML
    private void onStartChat() {
        String peer = peerField.getText() == null ? "" : peerField.getText().trim();
        if (!peer.isBlank() && mainController != null) {
            mainController.openChat(peer);
        } else {
            messageLabel.setText("请输入用户名");
        }
    }
}

