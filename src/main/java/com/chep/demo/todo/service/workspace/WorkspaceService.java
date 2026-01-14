package com.chep.demo.todo.service.workspace;

import com.chep.demo.todo.domain.project.Project;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceMemberRepository;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import com.chep.demo.todo.exception.workspace.WorkspaceAccessDeniedException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Workspace> getWorkspaces(Long userId) {
        return workspaceMemberRepository.findAllByUserIdAndStatus(userId, WorkspaceMember.Status.ACTIVE)
                .stream()
                .map(WorkspaceMember::getWorkspace)
                .toList();
    }

    public Workspace createWorkspace(Long ownerId, String name, String description) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("owner not found"));

        Workspace workspace = Workspace.of(owner, name, description);
        Workspace saved = workspaceRepository.save(workspace);
        workspaceMemberRepository.save(WorkspaceMember.owner(saved, owner));
        return saved;
    }

    public Workspace updateWorkspace(Long workspaceId, Long userId, String name, String description) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));

        ensureOwner(workspace, userId);
        workspace.changeNameAndDescription(name, description);
        return workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspace(Long workspaceId, Long userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));

        boolean isMember = workspaceMemberRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                userId,
                WorkspaceMember.Status.ACTIVE
        ).isPresent();

        if (!isMember) {
            throw new WorkspaceAccessDeniedException("Only workspace members can access this resource.");
        }

        return workspace;
    }

    public void deleteWorkspace(Long workspaceId, Long userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));

        ensureOwner(workspace, userId);
        if (workspace.isPersonal()) {
            throw new WorkspaceOperationException("Personal workspace cannot be deleted");
        }

        long activeMembers = workspaceMemberRepository.findAllByWorkspaceIdAndStatus(
                workspaceId,
                WorkspaceMember.Status.ACTIVE
        ).size();
        if (activeMembers > 1) {
            throw new WorkspaceOperationException("Cannot delete workspace while other members remain active.");
        }

        workspace.markDeleted();
        workspaceRepository.save(workspace);
    }

    private void ensureOwner(Workspace workspace, Long userId) {
        if (workspace.getOwner() == null || !workspace.getOwner().getId().equals(userId)) {
            throw new WorkspaceAccessDeniedException("You are not the owner of this workspace");
        }
    }
}
