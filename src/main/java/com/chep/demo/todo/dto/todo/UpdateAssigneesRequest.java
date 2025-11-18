package com.chep.demo.todo.dto.todo;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateAssigneesRequest(
        @NotNull(message = "assigneeIds is required")
        List<Long> assigneeIds
) {
}
