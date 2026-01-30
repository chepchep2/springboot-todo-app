package com.chep.demo.todo.exception.invitation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyWorkspaceMemberException extends RuntimeException {
    public AlreadyWorkspaceMemberException(String message) {
        super(message);
    }
}
