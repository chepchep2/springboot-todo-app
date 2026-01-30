package com.chep.demo.todo.exception.invitation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class InviteCodeExpiredException extends RuntimeException {
    public InviteCodeExpiredException(String message) {
        super(message);
    }
}
