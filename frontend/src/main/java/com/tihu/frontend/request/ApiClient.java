package com.tihu.frontend.request;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Array;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String baseUrl;
    private String token;
    private String sessionCookie;

    public ApiClient() {
        this(resolveBaseUrl());
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String token() {
        return token;
    }

    public void setToken(String token) {
        this.token = token == null || token.isBlank() ? null : token.trim();
    }

    public String sessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(String sessionCookie) {
        this.sessionCookie = sessionCookie;
    }

    public String get(String path) throws IOException, InterruptedException {
        return send("GET", path, null);
    }

    public String post(String path, String json) throws IOException, InterruptedException {
        return send("POST", path, json);
    }

    public String put(String path, String json) throws IOException, InterruptedException {
        return send("PUT", path, json);
    }

    public String delete(String path) throws IOException, InterruptedException {
        return send("DELETE", path, null);
    }

    public String send(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", token);
        }
        attachCookie(builder);

        switch (method.toUpperCase()) {
            case "POST" -> builder.header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            case "PUT" -> builder.header("Content-Type", "application/json; charset=UTF-8")
                    .PUT(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            case "DELETE" -> {
                if (body == null) {
                    builder.DELETE();
                } else {
                    builder.header("Content-Type", "application/json; charset=UTF-8");
                    builder.method("DELETE", HttpRequest.BodyPublishers.ofString(body));
                }
            }
            case "GET" -> builder.GET();
            default -> builder.method(method.toUpperCase(), body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        captureSession(response);
        return response.body();
    }

    private void attachCookie(HttpRequest.Builder builder) {
        if (sessionCookie != null && !sessionCookie.isBlank()) {
            builder.header("Cookie", sessionCookie);
        }
    }

    private void captureSession(HttpResponse<String> response) {
        String authHeader = response.headers().firstValue("Authorization").orElse(null);
        if (authHeader != null && !authHeader.isBlank()) {
            token = authHeader.trim();
        }
        String setCookie = response.headers().firstValue("Set-Cookie").orElse(null);
        if (setCookie != null && !setCookie.isBlank()) {
            sessionCookie = setCookie.split(";", 2)[0];
        }
    }

    public static String encodeQuery(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, ?> entry : new LinkedHashMap<>(params).entrySet()) {
            first = appendQueryValue(sb, first, entry.getKey(), entry.getValue());
        }
        return sb.toString();
    }

    private static boolean appendQueryValue(StringBuilder sb, boolean first, String key, Object value) {
        if (value == null) {
            return first;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                first = appendSingleQueryValue(sb, first, key, item);
            }
            return first;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                first = appendSingleQueryValue(sb, first, key, Array.get(value, i));
            }
            return first;
        }
        return appendSingleQueryValue(sb, first, key, value);
    }

    private static boolean appendSingleQueryValue(StringBuilder sb, boolean first, String key, Object value) {
        if (value == null) {
            return first;
        }
        if (!first) {
            sb.append('&');
        }
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        sb.append('=');
        sb.append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
        return false;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        if (value.isBlank()) {
            value = resolveBaseUrl();
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String resolveBaseUrl() {
        String sysProp = System.getProperty("tihu.backend.base-url");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String env = System.getenv("TIHU_BACKEND_BASE_URL");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return "http://localhost:22224/api";
    }
}
