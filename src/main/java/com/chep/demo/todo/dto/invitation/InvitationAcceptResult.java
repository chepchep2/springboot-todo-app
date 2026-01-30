package com.chep.demo.todo.dto.invitation;

import java.time.Instant;

public record InvitationAcceptResult(Result result, WorkspaceSummary workspace, MemberSummary member) {
    public enum Result {SUCCESS, ALREADY_MEMBER}
    public record WorkspaceSummary(Long id, String name) {}
    public record MemberSummary(Long id, String role, Instant joinedAt) {}
}
