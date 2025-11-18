package com.chep.demo.todo.dto.todo;

public record TodoResponse(
        Long id,
        String title,
        String content,
        boolean completed,
        Integer orderIndex
) {
}
