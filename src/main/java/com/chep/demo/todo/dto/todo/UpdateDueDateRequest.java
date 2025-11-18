package com.chep.demo.todo.dto.todo;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateDueDateRequest(
        @NotNull(message = "DueDate is required")
        Instant dueDate
) {
}
