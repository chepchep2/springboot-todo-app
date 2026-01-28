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
public class InvitationCodeUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invite_code_usage_id_gen")
    @SequenceGenerator(name = "invite_code_usage_id_gen", sequenceName = "invite_code_usage_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_code_id", nullable = false)
    private InvitationCode invitationCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_member_id", nullable = false)
    private WorkspaceMember workspaceMember;

    @NotNull
    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    protected InvitationCodeUsage() {}

    private InvitationCodeUsage(InvitationCode invitationCode, WorkspaceMember workspaceMember, Instant usedAt) {
        this.invitationCode = requireInvitationCode(invitationCode);
        this.workspaceMember = requireWorkspaceMember(workspaceMember);
        ensureSameWorkspace(this.invitationCode, this.workspaceMember);
        this.usedAt = usedAt != null ? usedAt : Instant.now();
    }

    public static InvitationCodeUsage record(InvitationCode invitationCode, WorkspaceMember workspaceMember, Instant now) {
        return new InvitationCodeUsage(invitationCode, workspaceMember, now);
    }

    public Long getId() {
        return id;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public InvitationCode getInvitationCode() {
        return invitationCode;
    }

    public WorkspaceMember getWorkspaceMember() {
        return workspaceMember;
    }

    private static InvitationCode requireInvitationCode(InvitationCode invitationCode) {
        if (invitationCode == null) {
            throw new InvitationValidationException("invitationCode must not be null");
        }
        return invitationCode;
    }

    private static WorkspaceMember requireWorkspaceMember(WorkspaceMember member) {
        if (member == null) {
            throw new InvitationValidationException("workspaceMember must not be null");
        }
        return member;
    }

    private static void ensureSameWorkspace(InvitationCode invitationCode, WorkspaceMember member) {
        if (invitationCode.getWorkspace() == null || member.getWorkspace() == null) {
            throw new InvitationValidationException("Workspace relation must be initialized");
        }

        Long inviteWorkspaceId = invitationCode.getWorkspace().getId();
        Long memberWorkspaceId = member.getWorkspace().getId();

        boolean matchesById = inviteWorkspaceId != null && memberWorkspaceId != null && Objects.equals(inviteWorkspaceId, memberWorkspaceId);
        boolean matchesByInstance = (inviteWorkspaceId == null || memberWorkspaceId == null) && invitationCode.getWorkspace() == member.getWorkspace();

        if (!(matchesById || matchesByInstance)) {
            throw new InvitationValidationException("Invitation code and workspace member must belong to the same workspace");
        }
    }
}