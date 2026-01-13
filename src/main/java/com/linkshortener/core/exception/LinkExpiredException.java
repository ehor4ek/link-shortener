package com.linkshortener.core.exception;

public class LinkExpiredException extends Exception {
    public LinkExpiredException(String message) {
        super(message);
    }
}