package com.chep.demo.todo.dto.workspace;

import java.time.Instant;
import java.util.List;

public record WorkspaceMemberCursorResponse(List<WorkspaceMemberResponse> members, boolean hasNext, Instant cursorJoinedAt, Long cursorMemberId) {
}
