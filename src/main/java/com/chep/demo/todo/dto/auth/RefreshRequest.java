package com.chep.demo.todo.dto.auth;

import jakarta.validation.constraints.NotEmpty;

public record RefreshRequest(
        @NotEmpty(message = "refreshToken is required")
        String refreshToken
) {
}
