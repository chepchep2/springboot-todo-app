package com.chep.demo.todo.dto.todo;

import java.time.Instant;
import java.util.List;

public record TodoResponse(
        Long id,
        String title,
        String content,
        boolean completed,
        Integer orderIndex,
        Instant dueDate,
        List<Long> assigneeIds
) {
}
