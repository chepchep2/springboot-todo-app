package com.chep.demo.todo.dto.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;
import com.chep.demo.todo.domain.invitation.InviteCode;

import java.util.List;

public record InvitationSendResult(InviteCode inviteCode, List<Invitation> invitations) {
    public static InvitationSendResult empty() {
        return new InvitationSendResult(null, List.of());
    }

    public boolean hasInvitations() {
        return inviteCode != null && invitations != null && !invitations.isEmpty();
    }
}
