package com.chep.demo.todo.dto.auth;

public record AuthResponse(
        Long id,
        String name,
        String email,
        String accessToken
) {
}
