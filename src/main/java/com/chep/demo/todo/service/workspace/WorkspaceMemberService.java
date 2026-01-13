package com.chep.demo.todo.service.workspace;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceMemberRepository;
import com.chep.demo.todo.dto.workspace.WorkspaceMemberPage;
import com.chep.demo.todo.dto.workspace.WorkspaceMemberResponse;
import com.chep.demo.todo.exception.workspace.WorkspaceAccessDeniedException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberOperationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class WorkspaceMemberService {
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public WorkspaceMemberService(WorkspaceMemberRepository workspaceMemberRepository,
                                  UserRepository userRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMember> getMembers(Long workspaceId, Long userId) {
        requireActiveMember(workspaceId, userId);
        return workspaceMemberRepository.findAllByWorkspaceIdAndStatus(workspaceId, WorkspaceMember.Status.ACTIVE);
    }

        @Transactional(readOnly = true)
        public WorkspaceMemberPage getMembersWithCursor(
                Long workspaceId,
                Long userId,
                WorkspaceMember.Status status,
                String cursorJoinedAt,
                Long cursorMemberId,
                String keyword,
                int limit
        ) {
            requireActiveMember(workspaceId, userId);

            WorkspaceMember.Status effectiveStatus = (status == null) ? WorkspaceMember.Status.ACTIVE : status;
            String effectiveStatusValue = effectiveStatus.name();

            if (limit <= 0) limit = 20;

            Pageable pageable = PageRequest.of(0, limit + 1, Sort.unsorted());

            Instant cursorInstant = null;
            if (cursorJoinedAt != null) {
                cursorInstant = Instant.parse(cursorJoinedAt);
            }

            List<WorkspaceMember> members = workspaceMemberRepository.findMembersWithCursor(
                    workspaceId,
                    effectiveStatusValue,
                    cursorInstant,
                    cursorMemberId,
                    keyword,
                    pageable
            );

            boolean hasNext = members.size() > limit;
            if (hasNext) {
                members = members.subList(0, limit);
            }
            Instant nextJoinedAt = null;
            Long nextMemberId = null;

            if (!members.isEmpty()) {
                WorkspaceMember last = members.get(members.size() - 1);
                nextJoinedAt = last.getJoinedAt();
                nextMemberId = last.getId();
            }

            return new WorkspaceMemberPage(members, hasNext, nextJoinedAt, nextMemberId);
        }

    public WorkspaceMember addMember(Long workspaceId, Long requesterId, Long targetUserId) {
        WorkspaceMember requester = requireActiveMember(workspaceId, requesterId);
        ensureOwner(requester);

        if (requester.getWorkspace().isPersonal()) {
            throw new WorkspaceMemberOperationException("Cannot add members to a personal workspace.");
        }

        if (workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .filter(member -> member.getStatus() == WorkspaceMember.Status.ACTIVE)
                .isPresent()) {
            throw new WorkspaceMemberOperationException("User is already a workspace member.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new WorkspaceMemberOperationException("User not found."));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .map(existing -> {
                    existing.restoreActive();
                    return existing;
                })
                .orElseGet(() -> WorkspaceMember.member(requester.getWorkspace(), targetUser));

        return workspaceMemberRepository.save(member);
    }

    public void removeMember(Long workspaceId, Long requesterId, Long memberId) {
        WorkspaceMember requester = requireActiveMember(workspaceId, requesterId);
        ensureOwner(requester);

        WorkspaceMember target = findMember(workspaceId, memberId);
        if (target.isOwner()) {
            throw new WorkspaceMemberOperationException("Workspace owner cannot be removed.");
        }

        target.kick();
        workspaceMemberRepository.save(target);
    }

    public void leave(Long workspaceId, Long userId) {
        WorkspaceMember member = requireActiveMember(workspaceId, userId);
        if (member.isOwner()) {
            throw new WorkspaceMemberOperationException("Workspace owner cannot leave directly.");
        }
        member.leave();
        workspaceMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public WorkspaceMember getMyMembership(Long workspaceId, Long userId) {
        return requireActiveMember(workspaceId, userId);
    }

    private WorkspaceMember requireActiveMember(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMember.Status.ACTIVE
                ).orElseThrow(() -> new WorkspaceAccessDeniedException("Only workspace members can perform this action."));
    }

    private WorkspaceMember findMember(Long workspaceId, Long memberId) {
        return workspaceMemberRepository.findById(memberId)
                .filter(member -> member.getWorkspace().getId().equals(workspaceId))
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace member not found."));
    }

    private void ensureOwner(WorkspaceMember member) {
        if (!member.isOwner()) {
            throw new WorkspaceAccessDeniedException("Only workspace owners can perform this action.");
        }
    }
}
