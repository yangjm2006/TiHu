package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ConversationsController implements MainContentController {

    @FXML private ListView<String> conversationListView;
    @FXML private TextField keywordField;
    @FXML private TextField peerField;
    @FXML private TextArea firstMessageArea;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private MainController mainController;
    private List<MockBackendService.ConversationPreview> allConversations = List.of();
    private List<MockBackendService.ConversationPreview> filteredConversations = List.of();

    @FXML
    private void initialize() {
        conversationListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenChat();
            }
        });
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onShow() {
        loadConversations();
    }

    @FXML
    private void onRefresh() {
        loadConversations();
    }

    @FXML
    private void onFilter() {
        applyFilter();
    }

    @FXML
    private void onClearFilter() {
        keywordField.clear();
        applyFilter();
    }

    private void loadConversations() {
        try {
            allConversations = context.service().listConversations(context.username());
            applyFilter();
            messageLabel.setText(allConversations.isEmpty() ? "暂无会话，可输入用户名发起私信" : "双击会话可打开对话");
        } catch (Exception ex) {
            allConversations = List.of();
            filteredConversations = List.of();
            conversationListView.getItems().clear();
            messageLabel.setText(ex.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim().toLowerCase(Locale.ROOT);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        filteredConversations = allConversations.stream()
                .filter(item -> keyword.isBlank()
                        || item.peer().toLowerCase(Locale.ROOT).contains(keyword)
                        || item.lastMessage().toLowerCase(Locale.ROOT).contains(keyword))
                .sorted(Comparator.comparing(MockBackendService.ConversationPreview::lastTime).reversed())
                .toList();
        conversationListView.getItems().setAll(filteredConversations.stream()
                .map(item -> item.peer() + " | " + item.lastTime().format(formatter) + " | " + abbreviate(item.lastMessage()))
                .toList());
    }

    @FXML
    private void onOpenChat() {
        int idx = conversationListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= filteredConversations.size()) {
            messageLabel.setText("请先选择会话");
            return;
        }
        if (mainController != null) {
            mainController.openChat(filteredConversations.get(idx).peer());
        }
    }

    @FXML
    private void onStartChat() {
        String peer = peerField.getText() == null ? "" : peerField.getText().trim();
        if (peer.isBlank()) {
            messageLabel.setText("请输入用户名");
            return;
        }
        if (peer.equals(context.username())) {
            messageLabel.setText("不能给自己发私信");
            return;
        }
        String firstMessage = firstMessageArea.getText() == null ? "" : firstMessageArea.getText().trim();
        try {
            if (!firstMessage.isBlank()) {
                context.service().sendMessage(context.username(), peer, firstMessage);
                firstMessageArea.clear();
            }
            if (mainController != null) {
                mainController.openChat(peer);
            }
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onOpenTypedPeerProfile() {
        String peer = peerField.getText() == null ? "" : peerField.getText().trim();
        if (!peer.isBlank() && mainController != null) {
            mainController.openUserProfile(peer);
        } else {
            messageLabel.setText("请输入用户名");
        }
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 60) {
            return normalized;
        }
        return normalized.substring(0, 60) + "...";
    }
}

