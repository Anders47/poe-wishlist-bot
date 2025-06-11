package com.anders.poewishlist.api;

/**
 * Exception thrown when an API call fails.
 */
public class ApiException extends Exception {
    private final int statusCode;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}