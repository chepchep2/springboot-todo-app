package com.chep.demo.todo.exception.invitation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InviteCodeNotFoundException extends RuntimeException {
    public InviteCodeNotFoundException(String message) {
        super(message);
    }
}
