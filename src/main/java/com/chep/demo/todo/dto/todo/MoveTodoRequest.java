package com.chep.demo.todo.dto.todo;

import jakarta.validation.constraints.NotNull;

public record MoveTodoRequest(
        @NotNull(message = "targetOrderIndex is required")
        Integer targetOrderIndex
) {
}
