package com.anders.poewishlist.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HttpStashApiClient.
 */
class HttpStashApiClientTest {
    private HttpClient mockHttpClient;
    private HttpStashApiClient client;
    private ObjectMapper objectMapper;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    void setUp() throws Exception {
        mockHttpClient = Mockito.mock(HttpClient.class);
        objectMapper = new ObjectMapper();
        mockResponse = Mockito.mock(HttpResponse.class);
        client = new HttpStashApiClient(mockHttpClient, objectMapper);
    }

    @Test
    void fetchWithoutCursorReturnsResponse() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"next_change_id\":\"abc123\", \"stashes\":[]} ");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        StashResponse actual = client.fetch("TestLeague", null);
        assertEquals("abc123", actual.getNextChangeId());
        assertNotNull(actual.getStashes());
    }

    @Test
    void fetchWithCursorAppendsIdParam() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"next_change_id\":\"def456\", \"stashes\":[]} ");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        client.fetch("MyLeague", "prevId123");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(captor.capture(), any());
        URI uri = captor.getValue().uri();
        assertTrue(uri.toString().contains("league=MyLeague"));
        assertTrue(uri.toString().contains("id=prevId123"));
    }

    @Test
    void fetchHandlesNonOkStatus() throws Exception {
        when(mockResponse.statusCode()).thenReturn(429);
        when(mockResponse.body()).thenReturn("{ } ");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ApiException ex = assertThrows(ApiException.class, () -> client.fetch("L", null));
        assertEquals(429, ex.getStatusCode());
    }

    @Test
    void fetchHandlesMalformedJson() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("not-json");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ApiException ex = assertThrows(ApiException.class, () -> client.fetch("L", null));
        assertEquals(200, ex.getStatusCode());
    }
}