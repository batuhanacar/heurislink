package com.batuhan.heurislink.exception;

public class ShortUrlInactiveException extends RuntimeException {

    public ShortUrlInactiveException(String shortCode) {
        super("Short URL is inactive for code: " + shortCode);
    }
}