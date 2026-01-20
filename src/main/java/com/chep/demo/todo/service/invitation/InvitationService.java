package com.chep.demo.todo.service.invitation;

import com.chep.demo.todo.domain.invitation.*;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import com.chep.demo.todo.dto.invitation.InvitationAcceptResult;
import com.chep.demo.todo.dto.invitation.InvitationItem;
import com.chep.demo.todo.dto.invitation.InviteCreateResult;
import com.chep.demo.todo.dto.invitation.InviteResendResult;
import com.chep.demo.todo.exception.auth.UserNotFoundException;
import com.chep.demo.todo.exception.invitation.InvitationValidationException;
import com.chep.demo.todo.exception.invitation.InviteCodeNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspacePolicyViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvitationService {
    private final InviteCodeRepository inviteCodeRepository;
    private final InvitationRepository invitationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final InviteCodeUsageRepository inviteCodeUsageRepository;

    public InvitationService(
            InviteCodeRepository inviteCodeRepository,
            InvitationRepository invitationRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            InviteCodeUsageRepository inviteCodeUsageRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
        this.invitationRepository = invitationRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.inviteCodeUsageRepository = inviteCodeUsageRepository;
    }

    public InviteCreateResult createInvitations(Long workspaceId, Long requesterUserId, List<String> emails, Integer expiresInDays) {
        // 1. workspace 조회 + owner 체크
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(""));
        if (workspace.isPersonal()) {
            throw new WorkspacePolicyViolationException("");
        }
        workspace.requireOwnerMember(requesterUserId);
        User owner = workspace.getOwner();
        // 2. 이메일 정규화 + 필터링
        int effectiveExpires = expiresInDays == null ? InviteCode.DEFAULT_EXPIRATION_DAYS : expiresInDays;

        Set<String> normalized = new HashSet<>();
        for (String email: emails) {
            normalized.add(Invitation.normalizeEmail(email));
        }
        if (normalized.size() > 20) {
            throw new InvitationValidationException("max 20 emails");
        }

        Set<String> activeEmails = workspace.getActiveMembers().stream()
                .map(m -> Invitation.normalizeEmail(m.getUser().getEmail()))
                .collect(Collectors.toSet());

        List<String> targetEmails = normalized.stream()
                .filter(e -> !activeEmails.contains(e))
                .toList();
        if (targetEmails.isEmpty()) {
            return new InviteCreateResult(List.of());
        }
        // 3. InviteCode 생성
        Instant now = Instant.now();
        InviteCode inviteCode = InviteCode.create(workspace, owner, effectiveExpires);
        inviteCodeRepository.save(inviteCode);
        // 4. Invitation 생성
        List<Invitation> invitations = targetEmails.stream()
                .map(email -> Invitation.create(owner, inviteCode, email, now))
                .toList();
        invitationRepository.saveAll(invitations);
        // 5. 메일 발송
        Map<Long, Boolean> sendResults = emailSender.sendInvitations();
        // 6. 성공한 것만 SENT 처리
        for (Invitation inv: invitations) {
            boolean success = sendResults.getOrDefault(inv.getId(), false);
            if (success) {
                inv.markSent(now);
            }
        }
        List<InvitationItem> items = invitations.stream()
                .map(inv -> new InvitationItem(
                        inv.getId(),
                        inv.getSentEmail(),
                        inv.getInviteCode().getCode(),
                        inv.getStatus().name(),
                        inv.getInviteCode().getExpiresAt(),
                        buildInviteUrl(inv.getInviteCode().getCode()),
                        inv.getSentAt()
                )).toList();

        return new InviteCreateResult(items);
    }

    public InviteResendResult resendInvitation(Long workspaceId, Long requesterUserId, String email) {
        Instant now = Instant.now();
        String normalizedEmail = Invitation.normalizeEmail(email);
        // 1. owner 체크
        Workspace workspace = workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));
        if (workspace.isPersonal()) {
            throw new WorkspacePolicyViolationException("Personal workspace cannot invite members");
        }
        workspace.requireOwnerMember(requesterUserId);
        // 2. 이미 ACTIVE 멤버면 resend 보낼 필요가 없다.
        boolean alreadyActive = workspace.getActiveMembers().stream()
                .map(m -> Invitation.normalizeEmail(m.getUser().getEmail()))
                .anyMatch(e -> e.equals(normalizedEmail));

        if (alreadyActive) {
            return new InviteResendResult(List.of());
        }
        // 3. 기존 invitation(PENDING/SENT) expire
        List<Invitation> existing = invitationRepository.findByInviteCodeWorkspaceIdAndSentEmailAndStatusIn(workspaceId, normalizedEmail, List.of(Invitation.Status.PENDING, Invitation.Status.SENT));

        for (Invitation inv : existing) {
            inv.expire(now);
        }
        invitationRepository.saveAll(existing);
        // 4. 새 InviteCode + Invitation 생성
        User owner = workspace.getOwner();
        InviteCode newCode = inviteCodeRepository.save(InviteCode.create(workspace, owner, InviteCode.DEFAULT_EXPIRATION_DAYS));
        Invitation newInvitation = invitationRepository.save(Invitation.create(owner, newCode, normalizedEmail, now));
        // 5. 메일 발송(일단은 응답)
        InvitationItem item = new InvitationItem(
                newInvitation.getId(),
                newInvitation.getSentEmail(),
                newCode.getCode(),
                newInvitation.getStatus().name(),
                newCode.getExpiresAt(),
                buildInviteUrl(newCode.getCode()),
                newInvitation.getSentAt()
        );
        return new InviteResendResult(List.of(item));
    }

    public InvitationAcceptResult acceptInvitation(String inviteCode, Long userId) {
        // 0. 입력 검증
        String code = inviteCode == null ? "" : inviteCode.trim();
        if (code.isEmpty()) {
            throw new InvitationValidationException("Invite code must not be blank");
        }
        Instant now = Instant.now();
        // 1. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        String email = Invitation.normalizeEmail(user.getEmail());
        // 2. InviteCode 조회 + 만료 체크
        InviteCode inviteCodeEntity = inviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new InviteCodeNotFoundException("Invite code not found"));
        inviteCodeEntity.ensureNotExpired(now);
        // 3. Invitation 조회
        Invitation invitation = invitationRepository.findByInviteCodeAndSentEmail(code, email)
                .orElseThrow(() -> new InvitationValidationException("Invitation does not exist for this email"));
        // 4. workspace 조회
        Long workspaceId = inviteCodeEntity.getWorkspace().getId();
        Workspace workspace = workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(""));
        // 5. WorkspaceMember 처리(ACTIVE/LEFT/KICKED)
        if (workspace.hasActiveMember(user.getId())) {
            WorkspaceMember active = workspace.requireActiveMember(user.getId());
            return toResult(InvitationAcceptResult.Result.ALREADY_MEMBER, workspace, active);
        }
        WorkspaceMember member = workspace.addMember(user);

        invitation.accept(email, now);

        try {
            inviteCodeUsageRepository.save(InviteCodeUsage.record(inviteCodeEntity, member, now));
        } catch (DataIntegrityViolationException ignored) {
            // 이미 기록된 usage
        }

        invitationRepository.save(invitation);
        // 7. 결과 DTO 반환
        return toResult(InvitationAcceptResult.Result.SUCCESS, workspace, member);
    }

    private InvitationAcceptResult toResult(InvitationAcceptResult.Result result, Workspace ws, WorkspaceMember member) {
        var w = new InvitationAcceptResult.WorkspaceSummary(ws.getId(), ws.getName());
        var m = new InvitationAcceptResult.MemberSummary(member.getId(), member.getRole().name(), member.getJoinedAt());
        return new InvitationAcceptResult(result, w, m);
    }

    @Value("${app.base-url}")
    private String baseUrl;
    private String buildInviteUrl(String code) {
        return baseUrl + "/invitations/" + code + "/accept";
    }
}
