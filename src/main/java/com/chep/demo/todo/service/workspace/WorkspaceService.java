package com.chep.demo.todo.service.workspace;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceMemberRepository;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import com.chep.demo.todo.dto.workspace.WorkspaceMemberCursorResponse;
import com.chep.demo.todo.dto.workspace.WorkspaceMemberResponse;
import com.chep.demo.todo.exception.workspace.WorkspaceAccessDeniedException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberOperationException;
import com.chep.demo.todo.exception.workspace.WorkspaceNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceOwnerNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
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
    public WorkspaceMemberCursorResponse getMembers(Long workspaceId, Long userId, WorkspaceMember.Status status, Instant cursorJoined, Long cursorMemberId, String keyword, int limit) {
        if (status == null) {
            status = WorkspaceMember.Status.ACTIVE;
        }

        Workspace workspace = findWorkspaceId(workspaceId);
        workspace.requireActiveMember(userId);

        Pageable pageable = PageRequest.of(0, limit + 1);

        List<WorkspaceMember> members = fetchMembers(workspaceId, status, cursorJoined, cursorMemberId, keyword, pageable);

        boolean hasNext = members.size() > limit;

        List<WorkspaceMember> resultMembers = hasNext ? members.subList(0, limit) : members;

        return buildResponse(resultMembers, hasNext);
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

    public void removeMember(Long workspaceId, Long requesterId, Long workspaceMemberId) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.requireOwnerMember(requesterId);

        if (workspaceMemberId == null) {
            throw new WorkspaceMemberNotFoundException("Workspace member not found.");
        }

        workspace.kickMember(workspaceMemberId);
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

    private Workspace findWorkspaceId(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));
    }

    private List<WorkspaceMember> fetchMembers(Long workspaceId, WorkspaceMember.Status status, Instant cursorJoined, Long cursorMemberId, String keyword, Pageable pageable) {
        if (cursorJoined == null) {
            if (keyword == null) {
                return workspaceMemberRepository.findFirstPage(workspaceId, status, pageable);
            } else {
                return workspaceMemberRepository.findFirstPageWithKeyword(workspaceId, status, keyword, pageable);
            }
        } else {
            if (keyword == null) {
                return workspaceMemberRepository.findNextPage(workspaceId, status, cursorJoined, cursorMemberId, pageable);
            } else {
                return workspaceMemberRepository.findNextPageWithKeyword(workspaceId, status, cursorJoined, cursorMemberId, keyword, pageable);
            }
        }
    }

    private WorkspaceMemberCursorResponse buildResponse(List<WorkspaceMember> resultMembers, boolean hasNext) {
        List<WorkspaceMemberResponse> memberResponses = resultMembers.stream()
                .map(m -> new WorkspaceMemberResponse(
                        m.getId(),
                        m.getUser().getId(),
                        m.getRole(),
                        m.getStatus(),
                        m.getJoinedAt(),
                        m.getStatusChangedAt()
                )).toList();

        Instant cursorJoinedAt = null;
        Long cursorMemberId = null;

        if (hasNext && !resultMembers.isEmpty()) {
            WorkspaceMember lastMember = resultMembers.getLast();
            cursorJoinedAt = lastMember.getJoinedAt();
            cursorMemberId = lastMember.getId();
        }

        return new WorkspaceMemberCursorResponse(
                memberResponses,
                hasNext,
                cursorJoinedAt,
                cursorMemberId
        );
    }
}
