package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.DateTimeUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.util.Comparator;

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
        if (peer == null || peer.isBlank()) {
            peer = "";
            titleLabel.setText("私信");
            messageLabel.setText("请先选择或输入私信对象");
            return;
        }
        titleLabel.setText("与 " + peer + " 的私信");
        refresh();
    }

    @FXML
    private void onSend() {
        try {
            String content = inputArea.getText() == null ? "" : inputArea.getText().trim();
            if (peer.isBlank()) {
                messageLabel.setText("请先选择私信对象");
                return;
            }
            if (peer.equals(context.username())) {
                messageLabel.setText("不能给自己发私信");
                return;
            }
            if (content.isBlank()) {
                messageLabel.setText("消息不能为空");
                return;
            }
            context.service().sendMessage(context.username(), peer, content);
            inputArea.clear();
            refresh();
            messageLabel.setText("已发送");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onOpenPeerProfile() {
        if (mainController != null && peer != null && !peer.isBlank()) {
            mainController.openUserProfile(peer);
        }
    }

    @FXML
    private void onBack() {
        if (mainController != null) {
            mainController.onConversations();
        }
    }

    private void refresh() {
        if (peer == null || peer.isBlank()) {
            messageListView.getItems().clear();
            return;
        }
        messageListView.getItems().setAll(context.service().listMessages(context.username(), peer).stream()
                .sorted(Comparator.comparing(MockBackendService.MessageItem::time))
                .map(item -> {
                    boolean mine = context.username().equals(item.from());
                    String sender = mine ? "我" : item.from();
                    return "[" + DateTimeUtil.format(item.time()) + "] " + sender + ": " + item.content();
                })
                .toList());
        if (!messageListView.getItems().isEmpty()) {
            messageListView.scrollTo(messageListView.getItems().size() - 1);
        }
    }
}

