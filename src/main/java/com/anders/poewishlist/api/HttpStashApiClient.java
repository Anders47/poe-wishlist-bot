package com.anders.poewishlist.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpStashApiClient implements StashApiClient {
    private static final String DEFAULT_BASE_URL = "https://www.pathofexile.com/api/stash/Standard";
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpStashApiClient() {
        this(DEFAULT_BASE_URL,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                new ObjectMapper());
    }

    public HttpStashApiClient(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public StashResponse fetch(String league, String nextChangeId) throws ApiException {
        String url = baseUrl;
        String contactEmail = System.getenv().getOrDefault("CONTACT_EMAIL", "default@email.com");
        String userAgent = "poe-wishlist-bot/1.0 (" + contactEmail + ")";
        if (nextChangeId != null) {
            url += "?id=" + nextChangeId;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(20))
                .build();

        int maxRetries = 5;
        int attempt = 0;
        int[] backoff = {5, 10, 20, 30, 60};

        while (true) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status == 200) {
                    try {
                        return objectMapper.readValue(response.body(), StashResponse.class);
                    } catch (IOException e) {
                        throw new ApiException(status, "Failed to parse stash response: " + e.getMessage());
                    }
                } else if (status == 429 && attempt < maxRetries) {
                    int wait = (attempt < backoff.length) ? backoff[attempt] : backoff[backoff.length - 1];
                    System.err.println("[WARN] Rate limited (429). Backing off " + wait + "s before retry " + (attempt + 1));
                    Thread.sleep(wait * 1000L);
                    attempt++;
                    continue;
                } else {
                    throw new ApiException(status, "Received non-OK status: " + status + " (body: " + response.body() + ")");
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("IOException caught: " + e.getMessage());
                throw new ApiException(-1, "Failed to call stash API: " + e.getMessage());
            }
        }
    }

}