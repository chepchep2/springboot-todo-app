package com.chep.demo.todo.dto.todo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateTodoRequest(
        @NotEmpty(message = "Title is required")
        String title,
        String content
) {
}
