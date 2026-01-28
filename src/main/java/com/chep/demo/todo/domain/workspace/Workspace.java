package com.chep.demo.todo.domain.workspace;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.exception.workspace.WorkspaceAccessDeniedException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspacePolicyViolationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "workspaces")
@SQLRestriction("deleted_at IS NULL")
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workspace_id_gen")
    @SequenceGenerator(name = "workspace_id_gen", sequenceName = "workspace_id_seq", allocationSize = 1)
    private Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "is_personal", nullable = false)
    private boolean personal;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<WorkspaceMember> members = new HashSet<>();

    protected Workspace() {}

    private Workspace(String name, String description, boolean personal) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        this.name = name;
        this.description = description;
        this.personal = personal;
        this.createdAt = Instant.now();
        this.updatedAt = null;

    }

    public static class Builder {
        private String name;
        private String description;
        private boolean personal;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder personal(boolean personal) {
            this.personal = personal;
            return this;
        }

        public Workspace build() {
            return new Workspace(name, description, personal);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Workspace personal(User owner) {
        Workspace ws = Workspace.builder()
                .name("Personal")
                .description("Personal Workspace")
                .personal(true)
                .build();

        ws.addOwner(owner);
        return ws;
    }

    public static Workspace of(User owner, String name, String description) {
        Workspace ws = Workspace.builder()
                .name(name)
                .description(description)
                .personal(false)
                .build();

        ws.addOwner(owner);
        return ws;
    }

    public void addOwner(User owner) {
        this.members.add(WorkspaceMember.owner(this, owner));
    }

    public void changeNameAndDescription(String name, String description) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        if (personal) {
            throw new WorkspacePolicyViolationException("Personal workspace cannot be deleted");
        }
        if (countActiveMembers() > 1) {
            throw new WorkspacePolicyViolationException("Cannot delete while other members remain active");
        }
        this.deletedAt = Instant.now();
    }

    public WorkspaceMember addMember(User user) {
        if (personal) {
            throw new WorkspacePolicyViolationException("Cannot modify members of a personal workspace");
        }
        return addOrRestoreMember(user);
    }

    public WorkspaceMember requireActiveMember(Long userId) {
        return members.stream()
                .filter(member -> member.hasUser(userId) && member.isActive())
                .findFirst()
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace member not found."));
    }

    public WorkspaceMember requireMember(Long memberId) {
        return members.stream()
                .filter(member -> Objects.equals(member.getId(), memberId))
                .findFirst()
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace member not found."));
    }

    public void requireOwnerMember(Long userId) {
        WorkspaceMember member = requireActiveMember(userId);
        if (!member.isOwner()) {
            throw new WorkspaceAccessDeniedException("only owner can perform this action");
        }
    }

    public List<WorkspaceMember> getActiveMembers() {
        return members.stream()
                .filter(WorkspaceMember::isActive)
                .toList();
    }

    public long countActiveMembers() {
        return members.stream()
                .filter(WorkspaceMember::isActive)
                .count();
    }

    public void kickMember(Long memberId) {
        WorkspaceMember member = requireMember(memberId);
        if (member.isOwner()) {
            throw new WorkspacePolicyViolationException("workspace owner cannot be removed");
        }
        member.kick();
    }

    public void leave(Long userId) {
        WorkspaceMember member = requireActiveMember(userId);
        if (member.isOwner()) {
            throw new WorkspacePolicyViolationException("workspace owner cannot leave directly");
        }
        member.leave();
    }

    public boolean hasActiveMember(Long userId) {
        return members.stream()
                .anyMatch(member -> member.hasUser(userId) && member.isActive());
    }

    public User getOwner() {
        return members.stream()
                .filter(WorkspaceMember::isOwner)
                .map(WorkspaceMember::getUser)
                .findFirst()
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace owner not found"));
    }

    public boolean isPersonal() {
        return personal;
    }

    public List<WorkspaceMember> getMembers() {
        return new ArrayList<>(members);
    }

    private WorkspaceMember addOrRestoreMember(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        Optional<WorkspaceMember> existing = members.stream()
                .filter(member -> member.hasUser(user.getId()))
                .findFirst();

        if (existing.isPresent()) {
            WorkspaceMember member = existing.get();
            if (member.isActive()) {
                throw new IllegalStateException("user is already an active member");
            }
            member.restoreActive();
            return member;
        }

        WorkspaceMember member = WorkspaceMember.member(this, user);
        members.add(member);
        return member;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
