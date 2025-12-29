package com.chep.demo.todo.dto.workspace;

public record WorkspaceResponse(
        Long id,
        String name,
        String description,
        boolean personal,
        Long ownerId
) {}
