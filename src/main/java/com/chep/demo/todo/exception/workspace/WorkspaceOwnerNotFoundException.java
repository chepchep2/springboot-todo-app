package com.chep.demo.todo.exception.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorkspaceOwnerNotFoundException extends RuntimeException {
    public WorkspaceOwnerNotFoundException(String message) {
        super(message);
    }
}
