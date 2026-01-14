package com.chep.demo.todo.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120)
        String name,
        @Size(max = 500)
        String description
) {}
