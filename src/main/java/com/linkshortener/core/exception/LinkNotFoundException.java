package com.linkshortener.core.exception;

public class LinkNotFoundException extends Exception {
    public LinkNotFoundException(String message) {
        super(message);
    }
}