package com.chep.demo.todo.dto;

import jakarta.validation.constraints.NotEmpty;

public record CreateTodoRequest(
        @NotEmpty(message = "Title is required")
        String title,
        String content
) {}