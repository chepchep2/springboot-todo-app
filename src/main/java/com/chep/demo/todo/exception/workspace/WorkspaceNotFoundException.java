package com.chep.demo.todo.exception.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorkspaceNotFoundException extends RuntimeException {
    public WorkspaceNotFoundException(String message) {
        super(message);
    }
}
