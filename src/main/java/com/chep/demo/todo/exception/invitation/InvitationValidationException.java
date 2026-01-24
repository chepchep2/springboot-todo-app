package com.chep.demo.todo.exception.invitation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvitationValidationException extends RuntimeException {
    public InvitationValidationException(String message) {
        super(message);
    }
}
