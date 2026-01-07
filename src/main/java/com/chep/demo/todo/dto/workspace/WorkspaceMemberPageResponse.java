package com.chep.demo.todo.dto.workspace;

import java.time.Instant;
import java.util.List;

public record WorkspaceMemberPageResponse(
        List<WorkspaceMemberResponse> members,
        boolean hasNext,
        Instant nextCursorJoinedAt,
        Long nextCursorMemberId
) {}
