package com.chep.demo.todo.dto.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;

import java.time.Instant;

public record InvitationSummaryDto(
        Long id,
        String sentEmail,
        Invitation.Status status,
        Instant sentAt,
        Instant acceptedAt
) {
    public static InvitationSummaryDto from(Invitation invitation) {
        return new InvitationSummaryDto(
                invitation.getId(),
                invitation.getSentEmail(),
                invitation.getStatus(),
                invitation.getSentAt(),
                invitation.getAcceptedAt()
        );
    }
}
