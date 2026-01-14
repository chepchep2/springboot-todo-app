package com.chep.demo.todo.dto.workspace;

import com.chep.demo.todo.domain.workspace.WorkspaceMember;

import java.time.Instant;

public record WorkspaceMemberResponse(
        Long id,
        Long userId,
        WorkspaceMember.Role role,
        WorkspaceMember.Status status,
        Instant joinedAt,
        Instant statusChangedAt
) {}
