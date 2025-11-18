package com.chep.demo.todo.dto.todo;

import java.time.Instant;

public record TodoResponse(
        Long id,
        String title,
        String content,
        boolean completed,
        Integer orderIndex,
        Instant dueDate
) {
}
