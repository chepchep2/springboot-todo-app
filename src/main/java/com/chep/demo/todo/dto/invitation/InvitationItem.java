package com.chep.demo.todo.dto.invitation;

import java.time.Instant;

public record InvitationItem(Long id, String sentEmail, String inviteCode, String status, Instant expiresAt, String inviteUrl, Instant sentAt) {
}
