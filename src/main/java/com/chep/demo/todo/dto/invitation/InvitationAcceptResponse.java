package com.chep.demo.todo.dto.invitation;

import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;

import java.time.Instant;

public record InvitationAcceptResponse(
        String result,
        WorkspaceSummary workspace,
        MemberSummary member
) {
    public static InvitationAcceptResponse from(InvitationAcceptResult result) {
        Workspace workspace = result.workspace();
        WorkspaceMember member = result.member();
        return new InvitationAcceptResponse(
                result.status().name(),
                workspace != null ? new WorkspaceSummary(workspace.getId(), workspace.getName()) : null,
                member != null ? new MemberSummary(member.getId(), member.getRole().name(), member.getJoinedAt()) : null
        );
    }

    public record WorkspaceSummary(Long id, String name) {}

    public record MemberSummary(Long id, String role, Instant joinedAt) {}
}
