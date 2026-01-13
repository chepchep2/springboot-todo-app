package com.chep.demo.todo.domain.workspace;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.exception.workspace.WorkspacePolicyViolationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
@Entity
@Table(
        name = "workspace_members",
        uniqueConstraints = @UniqueConstraint(name = "uq_wm_workspace_user", columnNames = {"workspace_id", "user_id"})
)
public class WorkspaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workspace_members_id_gen")
    @SequenceGenerator(name = "workspace_members_id_gen", sequenceName = "workspace_member_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @NotNull
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @NotNull
    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    protected WorkspaceMember() {}

    private WorkspaceMember(Workspace workspace, User user, Role role) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace must not be null");
        }

        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }

        this.workspace = workspace;
        this.user = user;
        this.role = role;

        this.status = Status.ACTIVE;
        this.joinedAt = Instant.now();
        this.statusChangedAt = Instant.now();
    }

    public static class Builder {
        private Workspace workspace;
        private User user;
        private Role role;

        public Builder workspace(Workspace workspace) {
            this.workspace = workspace;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        private Builder role(Role role) {
            this.role = role;
            return this;
        }

        public WorkspaceMember build() {
            return new WorkspaceMember(workspace, user, role);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WorkspaceMember owner(Workspace workspace, User user) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(Role.OWNER)
                .build();
    }

    public static WorkspaceMember member(Workspace workspace, User user) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(Role.MEMBER)
                .build();
    }

    public void leave() {
        if (this.role == Role.OWNER) {
            throw new WorkspacePolicyViolationException("owner cannot leave directly");
        }
        this.status = Status.LEFT;
        this.statusChangedAt = Instant.now();
    }

    public void kick() {
        this.status = Status.KICKED;
        this.statusChangedAt = Instant.now();
    }

    public void restoreActive() {
        this.status = Status.ACTIVE;
        this.statusChangedAt = Instant.now();
    }

    public boolean isOwner() {
        return this.role == Role.OWNER;
    }

    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }

    public boolean hasUser(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public enum Role {
        OWNER, MEMBER
    }

    public enum Status {
        ACTIVE, LEFT, KICKED
    }
}
