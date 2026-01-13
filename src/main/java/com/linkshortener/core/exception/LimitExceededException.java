package com.linkshortener.core.exception;

public class LimitExceededException extends Exception {
    public LimitExceededException(String message) {
        super(message);
    }
}