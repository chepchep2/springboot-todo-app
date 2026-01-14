package com.chep.demo.todo.dto.workspace;

import jakarta.validation.constraints.NotNull;

public record AddWorkspaceMemberRequest(
        @NotNull(message = "userId is required")
        Long userId
) {}
