package com.chep.demo.todo.service.workspace;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import com.chep.demo.todo.exception.workspace.WorkspaceAccessDeniedException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberOperationException;
import com.chep.demo.todo.exception.workspace.WorkspaceNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceOperationException;
import com.chep.demo.todo.exception.workspace.WorkspaceOwnerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Workspace> getMyWorkspaces(Long userId) {
        return workspaceRepository.findAllByMemberUserIdAndStatus(userId, WorkspaceMember.Status.ACTIVE);
    }

    public Workspace createWorkspace(Long ownerId, String name, String description) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new WorkspaceOwnerNotFoundException("Owner not found."));

        Workspace workspace = Workspace.of(owner, name, description);
        return workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMember> getMembers(Long workspaceId, Long userId) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.requireActiveMember(userId);
        return workspace.getActiveMembers();
    }

    @Transactional(readOnly = true)
    public WorkspaceMember getMyMembership(Long workspaceId, Long userId) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        return workspace.requireActiveMember(userId);
    }

    public WorkspaceMember addMember(Long workspaceId, Long requesterId, Long targetUserId) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.requireOwnerMember(requesterId);

        if (workspace.hasActiveMember(targetUserId)) {
            throw new WorkspaceMemberOperationException("User is already a workspace member.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new WorkspaceMemberOperationException("User not found."));

        WorkspaceMember added = workspace.addMember(targetUser);
        workspaceRepository.save(workspace);
        return added;
    }

    public void removeMember(Long workspaceId, Long requesterId, Long memberId) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.requireOwnerMember(requesterId);

        if (memberId == null) {
            throw new WorkspaceMemberNotFoundException("Workspace member not found.");
        }

        workspace.kickMember(memberId);
        workspaceRepository.save(workspace);
    }

    public void leave(Long workspaceId, Long userId) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.leave(userId);
        workspaceRepository.save(workspace);
    }

    public Workspace updateWorkspace(Long workspaceId, Long userId, String name, String description) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.requireOwnerMember(userId);
        workspace.changeNameAndDescription(name, description);
        return workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspace(Long workspaceId, Long userId) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);

        if (!workspace.hasActiveMember(userId)) {
            throw new WorkspaceAccessDeniedException("Only workspace members can access this resource.");
        }
        return workspace;
    }

    public void deleteWorkspace(Long workspaceId, Long userId) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.requireOwnerMember(userId);
        workspace.markDeleted();
        workspaceRepository.save(workspace);
    }

    private Workspace getWorkspaceWithMembers(Long workspaceId) {
        return workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));
    }
}
