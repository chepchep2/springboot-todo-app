package com.chep.demo.todo.exception.invitation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class InvitationCodeExpiredException extends RuntimeException {
    public InvitationCodeExpiredException(String message) {
        super(message);
    }
}
