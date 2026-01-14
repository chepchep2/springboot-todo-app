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
    private InviteCode inviteCode;

    @NotNull
    @Column(name = "sent_email", nullable = false, length = 320)
    private String sentEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

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

    private Invitation(Builder builder) {
        this.createdByUser = requireCreator(builder.createdByUser);
        this.inviteCode = requireInviteCode(builder.inviteCode);
        this.inviteCode.ensureNotExpired(Instant.now());
        this.sentEmail = normalizeEmail(builder.sentEmail);
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private User createdByUser;
        private String sentEmail;
        private InviteCode inviteCode;

        public Builder createdBy(User createdByUser) {
            this.createdByUser = createdByUser;
            return this;
        }

        public Builder sentEmail(String sentEmail) {
            this.sentEmail = sentEmail;
            return this;
        }

        public Builder inviteCode(InviteCode inviteCode) {
            this.inviteCode = inviteCode;
            return this;
        }

        public Invitation build() {
            return new Invitation(this);
        }
    }

    public boolean isExpired(Instant now) {
        return status == Status.EXPIRED || inviteCode.isExpired(now);
    }

    public void markSent() {
        requireStatus(Status.PENDING, "Only pending invitations can be marked as sent");
        this.status = Status.SENT;
        this.sentAt = Instant.now();
    }

    public void accept(String acceptingEmail) {
        requireStatus(Status.PENDING, Status.SENT);
        inviteCode.ensureNotExpired(Instant.now());
        String normalized = normalizeEmail(acceptingEmail);
        if (!Objects.equals(this.sentEmail, normalized)) {
            throw new InvitationValidationException("Invitation can only be accepted by the invited email address");
        }
        this.status = Status.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    public void expire() {
        if (this.status == Status.ACCEPTED) {
            throw new InvitationStateException("Accepted invitations cannot expire");
        }
        if (this.status == Status.EXPIRED) {
            return;
        }
        this.status = Status.EXPIRED;
        this.expiredAt = Instant.now();
    }

    public enum Status {
        PENDING,
        SENT,
        ACCEPTED,
        EXPIRED
    }

    public Long getId() {
        return id;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Workspace getWorkspace() { return inviteCode.getWorkspace(); }
    public String getSentEmail() { return sentEmail; }
    public Status getStatus() { return status; }
    public InviteCode getInviteCode() { return inviteCode; }
    public Instant getSentAt() { return sentAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getExpiredAt() { return expiredAt; }

    private static InviteCode requireInviteCode(InviteCode inviteCode) {
        if (inviteCode == null) {
            throw new InvitationValidationException("inviteCode must not be null");
        }
        return inviteCode;
    }

    private static User requireCreator(User user) {
        if (user == null) {
            throw new InvitationValidationException("createdBy user must not be null");
        }
        return user;
    }

    private static String normalizeEmail(String rawEmail) {
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

    private void requireStatus(Status allowedStatus, String message) {
        if (this.status != allowedStatus) {
            throw new InvitationStateException(message);
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
