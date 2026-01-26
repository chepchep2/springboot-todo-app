package com.chep.demo.todo.service.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;
import com.chep.demo.todo.domain.invitation.InvitationRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.service.email.InvitationEmailTemplate;
import com.chep.demo.todo.service.email.ResendEmailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class InvitationEmailAsyncService {
    private final ResendEmailSender resendEmailSender;
    private final InvitationLinkBuilder invitationLinkBuilder;
    private final InvitationEmailTxService invitationEmailTxService;

    public InvitationEmailAsyncService(
            ResendEmailSender resendEmailSender,
            InvitationLinkBuilder invitationLinkBuilder,
            InvitationEmailTxService invitationEmailTxService
    ) {
        this.resendEmailSender = resendEmailSender;
        this.invitationLinkBuilder = invitationLinkBuilder;
        this.invitationEmailTxService = invitationEmailTxService;
    }

    @Async("mailExecutor")
    public void sendInvitationEmail(Long invitationId) {
        Instant now = Instant.now();
        Optional<Invitation> optionalInvitation = invitationEmailTxService.getPendingInvitation(invitationId);
        if (optionalInvitation.isEmpty()) {
            return;
        }
        Invitation inv = optionalInvitation.get();

        String inviteUrl = invitationLinkBuilder.buildInviteUrl(inv.getInviteCode().getCode());

        Workspace workspace = inv.getWorkspace();
        var content = InvitationEmailTemplate.invite(workspace.getName(), inviteUrl);

        try {
            resendEmailSender.send(inv.getSentEmail(), content.subject(), content.html());
            invitationEmailTxService.markSent(invitationId, now);
        } catch(Exception e) {
            invitationEmailTxService.markFailed(invitationId);
        }
    }
}
