package com.chep.demo.todo.domain.invitation;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.exception.invitation.InvitationStateException;
import com.chep.demo.todo.exception.invitation.InvitationValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "invitations")
public class Invitation {
    private static final int EMAIL_MAX_LENGTH = 320;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invitation_id_gen")
    @SequenceGenerator(name = "invitation_id_gen", sequenceName = "invitation_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_code_id", nullable = false)
    private InvitationCode invitationCode;

    @NotNull
    @Column(name = "sent_email", nullable = false, length = EMAIL_MAX_LENGTH)
    private String sentEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    protected Invitation() {}

    private Invitation(
            User createdBy,
            InvitationCode inviteCode,
            String sentEmail,
            Status status,
            Instant createdAt,
            Instant sentAt,
            Instant acceptedAt,
            Instant expiredAt
    ) {
        this.createdBy = requireCreator(createdBy);
        this.invitationCode = requireInviteCode(invitationCode);
        this.sentEmail = normalizeEmail(sentEmail);
        this.status = status;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.acceptedAt = acceptedAt;
        this.expiredAt = expiredAt;
    }

    public static Invitation create(User createdByUser, InvitationCode invitationCode, String sentEmail, Instant now) {
        InvitationCode code = requireInviteCode(invitationCode);
        code.ensureNotExpired(now);

        return new Invitation(createdByUser, code, sentEmail, Status.PENDING, now, null, null, null);
    }

    public boolean isExpired(Instant now) {
        return status == Status.EXPIRED || invitationCode.isExpired(now);
    }

    public void markSent(Instant now) {
        requirePending();
        this.status = Status.SENT;
        this.sentAt = now;
    }

    public void markFailed() {
        this.status = Status.FAILED;
    }

    public void accept(String acceptingEmail, Instant now) {
        requireStatus(Status.PENDING, Status.SENT);
        invitationCode.ensureNotExpired(now);
        String normalized = normalizeEmail(acceptingEmail);
        if (!Objects.equals(this.sentEmail, normalized)) {
            throw new InvitationValidationException("Invitation can only be accepted by the invited email address");
        }
        this.status = Status.ACCEPTED;
        this.acceptedAt = now;
    }

    public void expire(Instant now) {
        if (this.status == Status.ACCEPTED) {
            throw new InvitationStateException("Accepted invitations cannot expire");
        }
        if (this.status == Status.EXPIRED) {
            return;
        }
        this.status = Status.EXPIRED;
        this.expiredAt = now;
    }

    public enum Status {
        PENDING,
        SENT,
        ACCEPTED,
        // 여기서 EXPIRED는 기간이 만료되서 EXPIRED가 아니라 재발송을 하면 기존 Invitation가 만료되서 EXPIRED. 나중에 용어 변경 예정
        EXPIRED,
        FAILED
    }

    public Long getId() {
        return id;
    }

    public User getCreatedByUser() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Workspace getWorkspace() { return invitationCode.getWorkspace(); }
    public String getSentEmail() { return sentEmail; }
    public Status getStatus() { return status; }
    public InvitationCode getInviteCode() { return invitationCode; }
    public Instant getSentAt() { return sentAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getExpiredAt() { return expiredAt; }

    private static InvitationCode requireInviteCode(InvitationCode invitationCode) {
        if (invitationCode == null) {
            throw new InvitationValidationException("inviteCode must not be null");
        }
        return invitationCode;
    }

    private static User requireCreator(User user) {
        if (user == null) {
            throw new InvitationValidationException("createdBy user must not be null");
        }
        return user;
    }

    public static String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            throw new InvitationValidationException("sentEmail must not be null");
        }

        String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new InvitationValidationException("sentEmail must not be blank");
        }
        if (normalized.length() > EMAIL_MAX_LENGTH) {
            throw new InvitationValidationException("sentEmail must not exceed " + EMAIL_MAX_LENGTH + " characters");
        }
        if (!normalized.contains("@")) {
            throw new InvitationValidationException("sentEmail must contain '@'");
        }
        return normalized;
    }

    private void requirePending() {
        if (this.status != Status.PENDING) {
            throw new InvitationStateException("Only pending invitations can be marked as sent");
        }
    }

    private void requireStatus(Status... allowedStatuses) {
        for (Status allowed : allowedStatuses) {
            if (this.status == allowed) {
                return;
            }
        }
        throw new InvitationStateException("Invitation is not in an allowed state");
    }
}