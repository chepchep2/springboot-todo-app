package com.chep.demo.todo.service.invitation.event;

import java.util.List;

public record InvitationsCreatedEvent(List<Long> invitationIds) {
}
