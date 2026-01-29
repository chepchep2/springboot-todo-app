package com.chep.demo.todo.domain.invitation;

import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.exception.invitation.InvitationValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "invitation_code_usages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_invite_code_usage_member", columnNames = "workspace_member_id")
        })
public class InviteCodeUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invite_code_usage_id_gen")
    @SequenceGenerator(name = "invite_code_usage_id_gen", sequenceName = "invite_code_usage_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_code_id", nullable = false)
    private InviteCode inviteCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_member_id", nullable = false)
    private WorkspaceMember workspaceMember;

    @NotNull
    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    protected InviteCodeUsage() {}

    private InviteCodeUsage(InviteCode inviteCode, WorkspaceMember workspaceMember, Instant usedAt) {
        this.inviteCode = requireInviteCode(inviteCode);
        this.workspaceMember = requireWorkspaceMember(workspaceMember);
        ensureSameWorkspace(this.inviteCode, this.workspaceMember);
        this.usedAt = usedAt != null ? usedAt : Instant.now();
    }

    public static InviteCodeUsage record(InviteCode inviteCode, WorkspaceMember workspaceMember, Instant now) {
        return new InviteCodeUsage(inviteCode, workspaceMember, now);
    }

    public Long getId() {
        return id;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public InviteCode getInviteCode() {
        return inviteCode;
    }

    public WorkspaceMember getWorkspaceMember() {
        return workspaceMember;
    }

    private static InviteCode requireInviteCode(InviteCode inviteCode) {
        if (inviteCode == null) {
            throw new InvitationValidationException("inviteCode must not be null");
        }
        return inviteCode;
    }

    private static WorkspaceMember requireWorkspaceMember(WorkspaceMember member) {
        if (member == null) {
            throw new InvitationValidationException("workspaceMember must not be null");
        }
        return member;
    }

    private static void ensureSameWorkspace(InviteCode inviteCode, WorkspaceMember member) {
        if (inviteCode.getWorkspace() == null || member.getWorkspace() == null) {
            throw new InvitationValidationException("Workspace relation must be initialized");
        }

        Long inviteWorkspaceId = inviteCode.getWorkspace().getId();
        Long memberWorkspaceId = member.getWorkspace().getId();

        boolean matchesById = inviteWorkspaceId != null && memberWorkspaceId != null && Objects.equals(inviteWorkspaceId, memberWorkspaceId);
        boolean matchesByInstance = (inviteWorkspaceId == null || memberWorkspaceId == null) && inviteCode.getWorkspace() == member.getWorkspace();

        if (!(matchesById || matchesByInstance)) {
            throw new InvitationValidationException("Invite code and workspace member must belong to the same workspace");
        }
    }
}