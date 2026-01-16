package com.chep.demo.todo.dto.invitation;

import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;

public record InvitationAcceptResult(AcceptStatus status, Workspace workspace, WorkspaceMember member) {
    public static InvitationAcceptResult joined(Workspace workspace, WorkspaceMember member) {
        return new InvitationAcceptResult(AcceptStatus.JOINED, workspace, member);
    }

    public static InvitationAcceptResult alreadyMember(Workspace workspace, WorkspaceMember member) {
        return new InvitationAcceptResult(AcceptStatus.ALREADY_MEMBER, workspace, member);
    }

    public enum AcceptStatus {
        JOINED,
        ALREADY_MEMBER
    }
}
