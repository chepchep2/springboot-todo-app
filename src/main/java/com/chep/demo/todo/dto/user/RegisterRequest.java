package com.chep.demo.todo.dto.user;

import jakarta.validation.constraints.NotEmpty;

public record RegisterRequest(
        @NotEmpty(message = "Email is required")
        String email,
        @NotEmpty(message = "Name is required")
        String name,
        @NotEmpty(message = "Password is required")
        @jakarta.validation.constraints.Size(min = 8, max = 12, message = "Password must be between 8 and 64 characters")
        String password
) {
}