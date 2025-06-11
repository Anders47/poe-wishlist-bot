package com.anders.poewishlist.api;

import com.anders.poewishlist.api.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Default implementation of StashApiClient using java.net.http.HttpClient and Jackson.
 */
public class HttpStashApiClient implements StashApiClient {
    private static final String BASE_URL = "https://www.pathofexile.com/api/public-stash-tabs";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the default client with a new HttpClient and ObjectMapper.
     */
    public HttpStashApiClient() {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                new ObjectMapper()
        );
    }

    /**
     * Constructs the client with provided HttpClient and ObjectMapper.
     * Enables dependency injection for testing.
     *
     * @param httpClient   the HttpClient to use
     * @param objectMapper the ObjectMapper to use
     */
    public HttpStashApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public StashResponse fetch(String league, String nextChangeId) throws ApiException {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("?league=").append(league);
        if (nextChangeId != null) {
            url.append("&id=").append(nextChangeId);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new ApiException(-1, "Failed to call stash API: " + e.getMessage());
        }

        int status = response.statusCode();
        if (status != 200) {
            throw new ApiException(status, "Received non-OK status: " + status);
        }

        try {
            return objectMapper.readValue(response.body(), StashResponse.class);
        } catch (IOException e) {
            throw new ApiException(status, "Failed to parse stash response: " + e.getMessage());
        }
    }
}