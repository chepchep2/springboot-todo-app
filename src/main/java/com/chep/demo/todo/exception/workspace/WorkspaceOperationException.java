package com.chep.demo.todo.exception.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WorkspaceOperationException extends RuntimeException {
    public WorkspaceOperationException(String message) {
        super(message);
    }
}
