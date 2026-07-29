package com.batuhan.heurislink.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Rate limit exceeded. Please try again later.");
    }
}