package com.tihu.frontend.controller;

public interface MainContentController {
    default void setMainController(MainController mainController) {
    }

    default void onShow() {
    }
}

