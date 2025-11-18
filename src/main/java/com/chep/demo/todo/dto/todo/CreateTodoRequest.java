package com.chep.demo.todo.dto.todo;

import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public record CreateTodoRequest(
        @NotEmpty(message = "Title is required")
        String title,
        String content,
        Integer orderIndex,
        Instant dueDate,
        List<Long> assigneeIds
) {}
