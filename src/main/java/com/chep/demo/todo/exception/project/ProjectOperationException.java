package com.chep.demo.todo.exception.project;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ProjectOperationException extends RuntimeException {
    public ProjectOperationException(String message) {
        super(message);
    }
}
