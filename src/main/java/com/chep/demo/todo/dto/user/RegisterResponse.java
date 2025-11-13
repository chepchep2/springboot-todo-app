package com.chep.demo.todo.dto.user;

public record RegisterResponse(
        Long id,
        String name,
        String email
) {
}
