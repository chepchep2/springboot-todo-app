package com.chep.demo.todo.service.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.service.email.InvitationEmailTemplate;
import com.chep.demo.todo.service.email.ResendEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Optional;

@Service
public class InvitationEmailService {
    private final ResendEmailSender resendEmailSender;
    private final InvitationLinkBuilder invitationLinkBuilder;
    private final InvitationEmailTxService invitationEmailTxService;

    public InvitationEmailService(
            ResendEmailSender resendEmailSender,
            InvitationLinkBuilder invitationLinkBuilder,
            InvitationEmailTxService invitationEmailTxService) {
        this.resendEmailSender = resendEmailSender;
        this.invitationLinkBuilder = invitationLinkBuilder;
        this.invitationEmailTxService = invitationEmailTxService;
    }
    private static final Logger log = LoggerFactory.getLogger(InvitationEmailService.class);

    public void sendInvitationEmail(Long invitationId) {
        Instant now = Instant.now();
        Optional<Invitation> optionalInvitation = invitationEmailTxService.getSendingInvitation(invitationId);
        if (optionalInvitation.isEmpty()) {
            log.warn("Failed to get SENDING invitation: invitationId={} (already processed or not found)", invitationId);
            return;
        }
        Invitation inv = optionalInvitation.get();

        if (inv.isExpired(now)) {
            invitationEmailTxService.markCancelled(invitationId);
            log.warn("Invitation expired, marked as CANCELLED: {}", invitationId);
            return;
        }

        String inviteUrl = invitationLinkBuilder.buildInviteUrl(inv.getInviteCode().getCode());

        Workspace workspace = inv.getInviteCode().getWorkspace();
        var content = InvitationEmailTemplate.invite(workspace.getName(), inviteUrl);

        try {
            resendEmailSender.send(inv.getSentEmail(), content.subject(), content.html());
            invitationEmailTxService.markSent(invitationId, now);
        } catch (WebClientResponseException e) {
            log.error("Resend API error. invitationId={}, status={}, body={}",
                    invitationId, e.getStackTrace(), e.getResponseBodyAsString(), e);
            invitationEmailTxService.markFailed(invitationId);
        } catch(Exception e) {
            log.error("Failed to send invitation email. invitationId={}, email={}",
                    invitationId, inv.getSentEmail(), e);
            invitationEmailTxService.markFailed(invitationId);
        }
    }
}
