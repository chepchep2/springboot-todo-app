package com.chep.demo.todo.service.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;
import com.chep.demo.todo.domain.invitation.InvitationRepository;
import com.chep.demo.todo.domain.invitation.InviteCode;
import com.chep.demo.todo.domain.invitation.InviteCodeRepository;
import com.chep.demo.todo.domain.invitation.InviteCodeUsage;
import com.chep.demo.todo.domain.invitation.InviteCodeUsageRepository;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import com.chep.demo.todo.domain.workspace.WorkspaceMemberRepository;
import com.chep.demo.todo.dto.invitation.InvitationAcceptResult;
import com.chep.demo.todo.dto.invitation.InvitationSendResult;
import com.chep.demo.todo.exception.invitation.InvitationValidationException;
import com.chep.demo.todo.exception.workspace.WorkspaceMemberNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class InvitationService {
    private static final int MAX_INVITE_EMAILS = 20;
    private static final List<Invitation.Status> RESEND_ELIGIBLE_STATUSES = List.of(
            Invitation.Status.PENDING,
            Invitation.Status.SENT
    );

    private final WorkspaceRepository workspaceRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final InvitationRepository invitationRepository;
    private final InviteCodeUsageRepository inviteCodeUsageRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public InvitationService(
            WorkspaceRepository workspaceRepository,
            InviteCodeRepository inviteCodeRepository,
            InvitationRepository invitationRepository,
            InviteCodeUsageRepository inviteCodeUsageRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.invitationRepository = invitationRepository;
        this.inviteCodeUsageRepository = inviteCodeUsageRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    public InvitationSendResult sendInvitations(Long workspaceId, Long requesterId, List<String> emails, Integer expiresInDays) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.requireOwnerMember(requesterId);

        User requester = getUser(requesterId);

        List<String> normalizedEmails = normalizeAndValidateEmails(emails);

        List<String> targetEmails = filterActiveMembers(workspace, normalizedEmails);

        if (targetEmails.isEmpty()) {
            return InvitationSendResult.empty();
        }

        int effectiveExpires = expiresInDays != null
                ? expiresInDays
                : InviteCode.DEFAULT_EXPIRATION_DAYS;

        InvitationSendResult result = createInvitations(workspace, requester, targetEmails, effectiveExpires);
        sendEmailsAndMarkSent(result);
        return result;
    }

    public InvitationSendResult resendInvitation(Long workspaceId, Long requesterId, String email) {
        Workspace workspace = getWorkspaceWithMembers(workspaceId);
        workspace.requireOwnerMember(requesterId);

        User requester = getUser(requesterId);
        String normalizedEmail = Invitation.normalizeEmail(email);

        if (isActiveMemberEmail(workspace, normalizedEmail)) {
            throw new InvitationValidationException("Email already belongs to an active member");
        }

        expireExistingInvitations(workspace.getId(), normalizedEmail);

        InvitationSendResult result = createInvitations(
                workspace,
                requester,
                List.of(normalizedEmail),
                InviteCode.DEFAULT_EXPIRATION_DAYS
        );
        sendEmailsAndMarkSent(result);
        return result;
    }

    public InvitationAcceptResult acceptInvitation(String inviteCode, Long userId) {
        String trimmedCode = inviteCode == null ? "" : inviteCode.trim();
        if (trimmedCode.isEmpty()) {
            throw new InvitationValidationException("Invite code must not be blank");
        }

        User user = getUser(userId);
        String normalizedEmail = Invitation.normalizeEmail(user.getEmail());

        InviteCode inviteCodeEntity = inviteCodeRepository.findByCode(trimmedCode)
                .orElseThrow(() -> new InvitationValidationException("Invite code not found"));
        inviteCodeEntity.ensureNotExpired(Instant.now());

        Invitation invitation = invitationRepository.findWithInviteCodeByCodeAndEmail(trimmedCode, normalizedEmail)
                .orElseThrow(() -> new InvitationValidationException("Invitation does not exist for this email"));

        Workspace workspace = getWorkspaceWithMembers(inviteCodeEntity.getWorkspace().getId());

        Optional<WorkspaceMember> existingMember = workspace.getMembers().stream()
                .filter(member -> member.getUser() != null && member.getUser().getId().equals(userId))
                .findFirst();

        if (existingMember.isPresent() && existingMember.get().isActive()) {
            return InvitationAcceptResult.alreadyMember(workspace, existingMember.get());
        }

        WorkspaceMember member = existingMember
                .map(existing -> {
                    existing.restoreActive();
                    return workspaceMemberRepository.save(existing);
                })
                .orElseGet(() -> {
                    WorkspaceMember newMember = WorkspaceMember.member(workspace, user);
                    workspace.getMembers().add(newMember);
                    return workspaceMemberRepository.save(newMember);
                });

        invitation.accept(user.getEmail());
        invitationRepository.save(invitation);

        inviteCodeUsageRepository.findByWorkspaceMemberId(member.getId())
                .orElseGet(() -> inviteCodeUsageRepository.save(InviteCodeUsage.builder()
                        .inviteCode(inviteCodeEntity)
                        .workspaceMember(member)
                        .build()));

        return InvitationAcceptResult.joined(workspace, member);
    }

    private void expireExistingInvitations(Long workspaceId, String normalizedEmail) {
        List<Invitation> invitations = invitationRepository.findByInviteCodeWorkspaceIdAndSentEmailAndStatusIn(
                workspaceId,
                normalizedEmail,
                RESEND_ELIGIBLE_STATUSES
        );

        if (invitations.isEmpty()) {
            return;
        }

        Collection<Long> ids = new HashSet<>();
        for (Invitation invitation : invitations) {
            ids.add(invitation.getId());
        }

        invitationRepository.bulkExpirePendingOrSent(ids, Instant.now());
    }

    private InvitationSendResult createInvitations(Workspace workspace,
                                                    User requester,
                                                    List<String> targetEmails,
                                                    int expiresInDays) {
        InviteCode inviteCode = inviteCodeRepository.save(InviteCode.generate(workspace, requester, expiresInDays));

        List<Invitation> invitations = new ArrayList<>();
        for (String email : targetEmails) {
            Invitation invitation = Invitation.builder()
                    .createdBy(requester)
                    .inviteCode(inviteCode)
                    .sentEmail(email)
                    .build();
            invitations.add(invitation);
        }

        List<Invitation> savedInvitations = invitationRepository.saveAll(invitations);
        return new InvitationSendResult(inviteCode, savedInvitations);
    }

    private void sendEmailsAndMarkSent(InvitationSendResult response) {
        if (!response.hasInvitations()) {
            return;
        }
        List<Invitation> invitations = response.invitations();
        for (Invitation invitation : invitations) {
            invitation.markSent();
        }
        invitationRepository.saveAll(invitations);
    }

    private Workspace getWorkspaceWithMembers(Long workspaceId) {
        return workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("User not found"));
    }

    private List<String> normalizeAndValidateEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            throw new InvitationValidationException("At least one email is required");
        }
        if (emails.size() > MAX_INVITE_EMAILS) {
            throw new InvitationValidationException("Cannot invite more than " + MAX_INVITE_EMAILS + " emails at once");
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String email : emails) {
            normalized.add(Invitation.normalizeEmail(email));
        }
        return new ArrayList<>(normalized);
    }

    private List<String> filterActiveMembers(Workspace workspace, List<String> normalizedEmails) {
        Set<String> activeMemberEmails = new HashSet<>();
        for (WorkspaceMember member : workspace.getMembers()) {
            if (!member.isActive()) {
                continue;
            }
            if (member.getUser() == null) {
                continue;
            }
            String memberEmail = Invitation.normalizeEmail(member.getUser().getEmail());
            activeMemberEmails.add(memberEmail);
        }

        List<String> targets = new ArrayList<>();
        for (String email : normalizedEmails) {
            if (!activeMemberEmails.contains(email)) {
                targets.add(email);
            }
        }
        return targets;
    }

    private boolean isActiveMemberEmail(Workspace workspace, String normalizedEmail) {
        for (WorkspaceMember member : workspace.getMembers()) {
            if (!member.isActive()) {
                continue;
            }
            if (member.getUser() == null) {
                continue;
            }
            String memberEmail = Invitation.normalizeEmail(member.getUser().getEmail());
            if (memberEmail.equals(normalizedEmail)) {
                return true;
            }
        }
        return false;
    }

}
