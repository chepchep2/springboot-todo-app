package com.chep.demo.todo.dto.workspace;

import com.chep.demo.todo.domain.workspace.WorkspaceMember;

import java.time.Instant;
import java.util.List;

public record WorkspaceMemberPage(
        List<WorkspaceMember> members,
        boolean hasNext,
        Instant nextCursorJoinedAt,
        Long nextCursorMemberId
) {}
