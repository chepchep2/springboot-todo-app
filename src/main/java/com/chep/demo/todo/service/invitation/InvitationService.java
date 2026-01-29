package com.chep.demo.todo.service.invitation;

import com.chep.demo.todo.domain.invitation.*;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceMemberRepository;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import com.chep.demo.todo.dto.invitation.InvitationAcceptResult;
import com.chep.demo.todo.dto.invitation.InvitationItem;
import com.chep.demo.todo.dto.invitation.InvitationResult;
import com.chep.demo.todo.exception.auth.UserNotFoundException;
import com.chep.demo.todo.exception.invitation.AlreadyWorkspaceMemberException;
import com.chep.demo.todo.exception.invitation.InvitationValidationException;
import com.chep.demo.todo.exception.invitation.InviteCodeNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspacePolicyViolationException;
import com.chep.demo.todo.service.invitation.event.InvitationsCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class InvitationService {
    private final InviteCodeRepository inviteCodeRepository;
    private final InvitationRepository invitationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final InviteCodeUsageRepository inviteCodeUsageRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final InvitationLinkBuilder invitationLinkBuilder;

    public InvitationService(
            InviteCodeRepository inviteCodeRepository,
            InvitationRepository invitationRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            InviteCodeUsageRepository inviteCodeUsageRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            ApplicationEventPublisher applicationEventPublisher,
            InvitationLinkBuilder invitationLinkBuilder
    ) {
        this.inviteCodeRepository = inviteCodeRepository;
        this.invitationRepository = invitationRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.inviteCodeUsageRepository = inviteCodeUsageRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.invitationLinkBuilder = invitationLinkBuilder;
    }
    private static final int EMAIL_MAX_COUNT = 20;

    public InvitationResult createInvitations(Long workspaceId, Long requesterUserId, List<String> emails, Integer expiresInDays) {
        // 1. workspace 조회 + owner 체크
        Workspace workspace = loadWorkspaceForOwner(workspaceId, requesterUserId);
        User owner = workspace.getOwner();

        int effectiveExpires = expiresInDays == null ? InviteCode.DEFAULT_EXPIRATION_DAYS : expiresInDays;
        InviteCode.validateExpirationDays(effectiveExpires);

        // 2. 이메일 정규화 + 필터링
        Set<String> activeEmails = new HashSet<>(
                workspaceMemberRepository.findActiveMemberEmails(workspaceId)
        );
        Set<String> pendingEmails = invitationRepository.findPendingOrSentEmails(workspaceId);
        List<String> targetEmails = filterTargetEmails(emails, activeEmails, pendingEmails);
        if (targetEmails.isEmpty()) {
            return new InvitationResult(List.of());
        }
        // 3. InviteCode, Invitation 생성
        List<Invitation> invitations = createAndSaveInvitations(workspace, owner, effectiveExpires, targetEmails);
        // 4. 메일 발송
        List<Long> ids = invitations.stream().map(Invitation::getId).toList();
        applicationEventPublisher.publishEvent(new InvitationsCreatedEvent(ids));

        return toCreateResult(invitations);
    }

    public InvitationResult resendInvitation(Long workspaceId, Long requesterUserId, String email) {
        Instant now = Instant.now();
        String normalizedEmail = Invitation.normalizeEmail(email);
        // 1. owner 체크
        Workspace workspace = loadWorkspaceForOwner(workspaceId, requesterUserId);
        // 2. 이미 ACTIVE 멤버면 resend 보낼 필요가 없다.
        validateNotActiveMember(workspaceId, normalizedEmail);
        // 3. 기존 invitation(PENDING/SENT) expire, 새 InviteCode + Invitation 생성
        Invitation newInvitation = recreateInvitation(workspace, normalizedEmail);
        // 4. 메일 발송
        applicationEventPublisher.publishEvent(new InvitationsCreatedEvent(List.of(newInvitation.getId())));

        return toCreateResult(List.of(newInvitation));
    }

    public InvitationAcceptResult acceptInvitation(Long workspaceId, String inviteCode, Long userId) {
        // 0. 입력 검증
        String code = validateAndNormalizeCode(inviteCode);
        Instant now = Instant.now();

        // 1. User 조회
        User user = loadUser(userId);
        String email = Invitation.normalizeEmail(user.getEmail());

        // 2. InviteCode 조회 + 만료 체크
        InviteCode inviteCodeEntity = validateInviteCode(code, workspaceId, now);

        // 3. Invitation 조회
        Invitation invitation = loadInvitation(code, email);

        // 4. workspace 조회
        Workspace workspace = loadWorkspace(workspaceId);

        // 5. WorkspaceMember 처리(ACTIVE/LEFT/KICKED)
        if (workspace.hasActiveMember(user.getId())) {
            WorkspaceMember active = workspace.requireActiveMember(user.getId());
            return toResult(InvitationAcceptResult.Result.ALREADY_MEMBER, workspace, active);
        }

        WorkspaceMember member = acceptAndJoin(workspace, user, invitation, inviteCodeEntity, email, now);

        // 7. 결과 DTO 반환
        return toResult(InvitationAcceptResult.Result.SUCCESS, workspace, member);
    }

    private InvitationAcceptResult toResult(InvitationAcceptResult.Result result, Workspace ws, WorkspaceMember member) {
        var w = new InvitationAcceptResult.WorkspaceSummary(ws.getId(), ws.getName());
        var m = new InvitationAcceptResult.MemberSummary(member.getId(), member.getRole().name(), member.getJoinedAt());
        return new InvitationAcceptResult(result, w, m);
    }

    private WorkspaceMember saveMember(Workspace workspace, User user) {
        try {
            WorkspaceMember member = workspace.addMember(user);
            return workspaceMemberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyWorkspaceMemberException("ALREADY MEMBER");
        }
    }

    private Workspace loadWorkspaceForOwner(Long workspaceId, Long userId) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));
        if (workspace.isPersonal()) {
            throw new WorkspacePolicyViolationException("Personal workspace cannot invite members");
        }
        workspace.requireOwnerMember(userId);
        return workspace;
    }

    private List<String> filterTargetEmails(List<String> emails, Set<String> activeEmails, Set<String> pendingEmails) {
        if (emails == null || emails.isEmpty()) {
            throw new InvitationValidationException("emails must not be empty");
        }

        Set<String> normalized = new HashSet<>();

        for (String email: emails) {
            normalized.add(Invitation.normalizeEmail(email));
        }

        if (normalized.size() > EMAIL_MAX_COUNT) {
            throw new InvitationValidationException("max " + EMAIL_MAX_COUNT + " emails");
        }

        return normalized.stream()
                .filter(e -> !activeEmails.contains(e))
                .filter(e -> !pendingEmails.contains(e))
                .toList();
    }

    private List<Invitation> createAndSaveInvitations(Workspace workspace, User owner, int effectiveExpires, List<String> targetEmails) {
        Instant now = Instant.now();
        InviteCode inviteCode = InviteCode.create(workspace, owner, effectiveExpires);
        inviteCodeRepository.save(inviteCode);

        List<Invitation> invitations = targetEmails.stream()
                .map(email -> Invitation.create(owner, inviteCode, email, now))
                .toList();
        invitations = invitationRepository.saveAll(invitations);
        return invitations;
    }

    private void validateNotActiveMember(Long workspaceId, String normalizedEmail) {
        Set<String> activeEmails = new HashSet<>(
                workspaceMemberRepository.findActiveMemberEmails(workspaceId)
        );

        if (activeEmails.contains(normalizedEmail)) {
            throw new InvitationValidationException("Email already belongs to an active member");
        }
    }

    private Invitation recreateInvitation(Workspace workspace, String normalizedEmail) {
        Instant now = Instant.now();

        invitationRepository.bulkCancelPendingOrSent(
                workspace.getId(),
                normalizedEmail,
                now
        );

        User owner = workspace.getOwner();
        InviteCode newCode = inviteCodeRepository.save(
                InviteCode.create(workspace, owner, InviteCode.DEFAULT_EXPIRATION_DAYS)
        );

        return invitationRepository.save(
                Invitation.create(owner, newCode, normalizedEmail, now)
        );
    }

    private InvitationResult toCreateResult(List<Invitation> invitations) {
        List<InvitationItem> items = invitations.stream()
                .map(inv -> new InvitationItem(
                        inv.getId(),
                        inv.getSentEmail(),
                        inv.getInviteCode().getCode(),
                        inv.getStatus().name(),
                        inv.getInviteCode().getExpiresAt(),
                        invitationLinkBuilder.buildInviteUrl(inv.getInviteCode().getCode()),
                        inv.getSentAt()
                ))
                .toList();

        return new InvitationResult(items);
    }

    private String validateAndNormalizeCode(String inviteCode) {
        String code = inviteCode == null ? "" : inviteCode.trim();
        if (code.isEmpty()) {
            throw new InvitationValidationException("Invite code must not be blank");
        }
        return code;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private InviteCode validateInviteCode(String code, Long workspaceId, Instant now) {
        InviteCode inviteCodeEntity = inviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new InviteCodeNotFoundException("Invite code not found"));

        inviteCodeEntity.ensureNotExpired(now);

        if (!inviteCodeEntity.getWorkspace().getId().equals(workspaceId)) {
            throw new InviteCodeNotFoundException("Invite code not found in this workspace");
        }

        return inviteCodeEntity;
    }

    private Invitation loadInvitation(String code, String email) {
        return invitationRepository.findByInviteCodeCodeAndSentEmail(code, email)
                .orElseThrow(() -> new InvitationValidationException("Invitation does not exist for this email"));
    }

    private Workspace loadWorkspace(Long workspaceId) {
        return workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));
    }

    private WorkspaceMember acceptAndJoin(Workspace workspace, User user, Invitation invitation, InviteCode inviteCodeEntity, String email, Instant now) {
        WorkspaceMember member = saveMember(workspace, user);

        invitation.accept(email, now);
        invitationRepository.save(invitation);

        inviteCodeUsageRepository.saveIfNotExists(InviteCodeUsage.record(inviteCodeEntity, member, now));

        return member;
    }
}
