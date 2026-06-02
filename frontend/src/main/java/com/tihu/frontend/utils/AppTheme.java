package com.tihu.frontend.utils;

import com.tihu.frontend.MainApplication;
import javafx.scene.Parent;
import javafx.scene.Scene;

public final class AppTheme {
    private static final String THEME_PATH = "app-theme.css";
    private static final String DARK_THEME_CLASS = "theme-dark";
    private static boolean darkMode;

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
        applyMode(scene);
    }

    public static void toggle(Scene scene) {
        darkMode = !darkMode;
        applyMode(scene);
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static String toggleButtonText() {
        return darkMode ? "日间模式" : "夜间模式";
    }

    private static void applyMode(Scene scene) {
        if (scene == null) {
            return;
        }
        Parent root = scene.getRoot();
        if (root == null) {
            return;
        }
        root.getStyleClass().remove(DARK_THEME_CLASS);
        if (darkMode) {
            root.getStyleClass().add(DARK_THEME_CLASS);
        }
    }
}
