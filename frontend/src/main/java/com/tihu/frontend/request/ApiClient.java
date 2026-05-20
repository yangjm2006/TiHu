package com.tihu.frontend.request;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String baseUrl;
    private String sessionCookie;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String sessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(String sessionCookie) {
        this.sessionCookie = sessionCookie;
    }

    public String get(String path) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).GET();
        attachCookie(builder);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        captureCookie(response);
        return response.body();
    }

    public String post(String path, String json) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json));
        attachCookie(builder);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        captureCookie(response);
        return response.body();
    }

    private void attachCookie(HttpRequest.Builder builder) {
        if (sessionCookie != null && !sessionCookie.isBlank()) {
            builder.header("Cookie", sessionCookie);
        }
    }

    private void captureCookie(HttpResponse<String> response) {
        String setCookie = response.headers().firstValue("Set-Cookie").orElse(null);
        if (setCookie != null && !setCookie.isBlank()) {
            sessionCookie = setCookie.split(";", 2)[0];
        }
    }
}

