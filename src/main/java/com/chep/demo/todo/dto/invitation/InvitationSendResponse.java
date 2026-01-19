package com.chep.demo.todo.dto.invitation;

import java.time.Instant;
import java.util.List;

public record InvitationSendResponse(
        String inviteCode,
        Instant expiresAt,
        List<InvitationSummaryDto> invitations
) {
}
