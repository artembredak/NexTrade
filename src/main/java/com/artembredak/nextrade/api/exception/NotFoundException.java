package com.artembredak.nextrade.api.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resourceName, Object id) {
        super(resourceName + " not found with id: " + id);
    }
}
