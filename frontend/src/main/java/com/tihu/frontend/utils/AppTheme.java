package com.tihu.frontend.utils;

import com.tihu.frontend.MainApplication;
import javafx.scene.Scene;

public final class AppTheme {
    private static final String THEME_PATH = "app-theme.css";

    private AppTheme() {
    }

    public static Scene scene(javafx.scene.Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        apply(scene);
        return scene;
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        var themeUrl = MainApplication.class.getResource(THEME_PATH);
        if (themeUrl == null) {
            return;
        }
        String theme = themeUrl.toExternalForm();
        if (!scene.getStylesheets().contains(theme)) {
            scene.getStylesheets().add(theme);
        }
    }
}
