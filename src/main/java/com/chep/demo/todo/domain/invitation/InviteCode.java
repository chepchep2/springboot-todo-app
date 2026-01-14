package com.chep.demo.todo.domain.invitation;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.exception.invitation.InvitationValidationException;
import com.chep.demo.todo.exception.invitation.InviteCodeExpiredException;
import com.chep.demo.todo.exception.workspace.WorkspaceAccessDeniedException;
import com.chep.demo.todo.exception.workspace.WorkspacePolicyViolationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "invite_codes")
public class InviteCode {
    public static final int DEFAULT_EXPIRATION_DAYS = 7;
    public static final int MIN_EXPIRATION_DAYS = 1;
    public static final int MAX_EXPIRATION_DAYS = 30;
    private static final int CODE_LENGTH = 16;
    private static final char[] CODE_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invite_code_id_gen")
    @SequenceGenerator(name = "invite_code_id_gen", sequenceName = "invite_code_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @NotNull
    @Column(name = "code", nullable = false, length = 16, unique = true)
    private String code;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InviteCode() {}

    private InviteCode(Builder builder) {
        this.workspace = requireWorkspace(builder.workspace);
        ensureInvitesAllowed(this.workspace);
        this.createdBy = requireCreator(builder.createdBy);
        ensureOwner(this.workspace, this.createdBy);
        this.code = requireCode(builder.code);
        this.expiresAt = requireExpiry(builder.expiresAt);
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        if (!this.expiresAt.isAfter(this.createdAt)) {
            throw new InvitationValidationException("expiresAt must be after createdAt");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Workspace workspace;
        private User createdBy;
        private String code;
        private Instant expiresAt;
        private Instant createdAt;

        public Builder workspace(Workspace workspace) {
            this.workspace = workspace;
            return this;
        }

        public Builder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public InviteCode build() {
            return new InviteCode(this);
        }
    }

    public static InviteCode generate(Workspace workspace, User createdBy) {
        return generate(workspace, createdBy, DEFAULT_EXPIRATION_DAYS);
    }

    public static InviteCode generate(Workspace workspace, User createdBy, int expiresInDays) {
        validateExpirationDays(expiresInDays);
        Instant expiresAt = Instant.now().plus(Duration.ofDays(expiresInDays));
        return InviteCode.builder()
                .workspace(workspace)
                .createdBy(createdBy)
                .code(generateRandomCode())
                .expiresAt(expiresAt)
                .build();
    }

    public static void validateExpirationDays(int expiresInDays) {
        if (expiresInDays < MIN_EXPIRATION_DAYS) {
            throw new InvitationValidationException("Expiration days must be at least " + MIN_EXPIRATION_DAYS);
        }
        if (expiresInDays > MAX_EXPIRATION_DAYS) {
            throw new InvitationValidationException("Expiration days must not exceed " + MAX_EXPIRATION_DAYS);
        }
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void ensureNotExpired(Instant now) {
        if (isExpired(now)) {
            throw new InviteCodeExpiredException("Invite code has expired");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    private static Workspace requireWorkspace(Workspace workspace) {
        if (workspace == null) {
            throw new InvitationValidationException("workspace must not be null");
        }
        return workspace;
    }

    private static void ensureInvitesAllowed(Workspace workspace) {
        if (workspace.isPersonal()) {
            throw new WorkspacePolicyViolationException("Personal workspace cannot issue invitations");
        }
    }

    private static User requireCreator(User user) {
        if (user == null) {
            throw new InvitationValidationException("createdBy must not be null");
        }
        return user;
    }

    private static void ensureOwner(Workspace workspace, User creator) {
        User owner = workspace.getOwner();
        if (owner == null) {
            throw new InvitationValidationException("workspace owner must be present");
        }

        Long ownerId = owner.getId();
        Long creatorId = creator.getId();
        boolean ownerMatchesById = ownerId != null && creatorId != null && Objects.equals(ownerId, creatorId);
        boolean ownerMatchesByInstance = (ownerId == null || creatorId == null) && owner == creator;

        if (!(ownerMatchesById || ownerMatchesByInstance)) {
            throw new WorkspaceAccessDeniedException("Only the workspace owner can issue invitations");
        }
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvitationValidationException("invite code must not be blank");
        }
        if (code.length() != CODE_LENGTH) {
            throw new InvitationValidationException("invite code must be exactly " + CODE_LENGTH + " characters");
        }
        for (char ch : code.toCharArray()) {
            if (!Character.isLetterOrDigit(ch)) {
                throw new InvitationValidationException("invite code must be alphanumeric");
            }
        }
        return code;
    }

    private static Instant requireExpiry(Instant expiresAt) {
        if (expiresAt == null) {
            throw new InvitationValidationException("expiresAt must not be null");
        }
        return expiresAt;
    }

    private static String generateRandomCode() {
        char[] result = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            result[i] = CODE_CHARSET[SECURE_RANDOM.nextInt(CODE_CHARSET.length)];
        }
        return new String(result);
    }
}
