package com.chep.demo.todo.dto.todo;

import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;

public record UpdateTodoRequest(
        @NotEmpty(message = "Title is required")
        String title,
        String content,
        Integer orderIndex,
        Instant dueDate
) {
}
