package com.imaginamos.todo.exception;

public class InvalidSortParameterException extends RuntimeException {

    public InvalidSortParameterException(String property) {
        super("Unsupported sort field: " + property);
    }
}
