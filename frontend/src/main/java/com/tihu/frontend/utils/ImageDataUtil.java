package com.tihu.frontend.utils;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;

public final class ImageDataUtil {
    private ImageDataUtil() {
    }

    public static String toDataUri(File file) throws IOException {
        if (file == null) {
            return null;
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        return mimeType(file) + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    public static Image image(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("data:")) {
            int commaIndex = normalized.indexOf(',');
            if (commaIndex < 0) {
                return null;
            }
            try {
                byte[] bytes = Base64.getDecoder().decode(normalized.substring(commaIndex + 1));
                return new Image(new ByteArrayInputStream(bytes));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return new Image(normalized, true);
    }

    private static String mimeType(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "data:image/jpeg";
        }
        if (name.endsWith(".gif")) {
            return "data:image/gif";
        }
        if (name.endsWith(".webp")) {
            return "data:image/webp";
        }
        return "data:image/png";
    }
}
