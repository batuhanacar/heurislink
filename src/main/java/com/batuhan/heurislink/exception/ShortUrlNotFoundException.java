package com.batuhan.heurislink.exception;

public class ShortUrlNotFoundException extends RuntimeException {

    public ShortUrlNotFoundException(String shortCode) {
        super("Short URL not found for code: " + shortCode);
    }
}