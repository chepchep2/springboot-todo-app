package com.chep.demo.todo.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100)
        String name,
        @Size(max = 500)
        String description
) {}
