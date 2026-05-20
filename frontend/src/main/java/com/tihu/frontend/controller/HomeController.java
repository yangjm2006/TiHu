package com.tihu.frontend.controller;

import com.tihu.frontend.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController implements MainContentController {

    @FXML private Label welcomeLabel;

    private final AppContext context = AppContext.getInstance();

    @Override
    public void onShow() {
        welcomeLabel.setText("欢迎回来，" + context.username() + "。请从左侧进入各模块。");
    }
}

