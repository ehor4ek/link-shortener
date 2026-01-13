package com.linkshortener.core.exception;

public class InvalidUrlException extends Exception {
    public InvalidUrlException(String message) {
        super(message);
    }
}