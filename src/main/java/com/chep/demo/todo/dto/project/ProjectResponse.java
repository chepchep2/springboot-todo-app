package com.chep.demo.todo.dto.project;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long workspaceId,
        Long createdBy,
        boolean defaultProject
) {}
