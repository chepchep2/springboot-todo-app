package com.chep.demo.todo.exception.todo;

public class TodoNotFoundException extends RuntimeException {
    public TodoNotFoundException(String message) {
        super(message);
    }
}
