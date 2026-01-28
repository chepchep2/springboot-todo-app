package com.chep.demo.todo.domain.invitation;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.exception.invitation.InvitationValidationException;
import com.chep.demo.todo.exception.invitation.InvitationCodeExpiredException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "invitation_codes")
public class InvitationCode {
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
    @Column(name = "code", nullable = false, length = CODE_LENGTH, unique = true)
    private String code;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvitationCode() {}

    private InvitationCode(Workspace workspace, User createdBy, String code, Instant expiresAt, Instant createdAt) {
        this.workspace = requireWorkspace(workspace);
        this.createdBy = requireCreator(createdBy);
        this.code = requireCode(code);
        this.expiresAt = requireExpiry(expiresAt);
        this.createdAt = createdAt != null ? createdAt : Instant.now();

        if (!this.expiresAt.isAfter(this.createdAt)) {
            throw new InvitationValidationException("expiresAt must be after createdAt");
        }
    }

    // TODO: application Layer에서 now 주입하도록 변경 후 이 주석 제거
    public static InvitationCode create(Workspace workspace, User createdBy, int expiresInDays, Instant now) {
        validateExpirationDays(expiresInDays);

        Instant expiresAt = now.plus(Duration.ofDays(expiresInDays));

        return new InvitationCode(workspace, createdBy, generateRandomCode(), expiresAt, now);
    }

    public static InvitationCode create(Workspace workspace, User createdBy, Instant now) {
        return create(workspace, createdBy, DEFAULT_EXPIRATION_DAYS, now);
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
            throw new InvitationCodeExpiredException("Invitation code has expired");
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static User requireCreator(User user) {
        if (user == null) {
            throw new InvitationValidationException("createdBy must not be null");
        }
        return user;
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvitationValidationException("invitation code must not be blank");
        }
        if (code.length() != CODE_LENGTH) {
            throw new InvitationValidationException("invitation code must be exactly " + CODE_LENGTH + " characters");
        }
        for (char ch : code.toCharArray()) {
            if (!Character.isLetterOrDigit(ch)) {
                throw new InvitationValidationException("invitation code must be alphanumeric");
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