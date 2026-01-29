package com.chep.demo.todo.service.invitation.processor;

import com.chep.demo.todo.domain.invitation.InvitationRepository;
import com.chep.demo.todo.service.invitation.InvitationEmailService;
import com.chep.demo.todo.service.invitation.InvitationEmailTxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
@Service
public class InvitationProcessor {
    private static final Logger log = LoggerFactory.getLogger(InvitationProcessor.class);
    private final InvitationRepository invitationRepository;
    private final InvitationEmailService invitationEmailService;
    private final InvitationEmailTxService invitationEmailTxService;

    public InvitationProcessor(InvitationRepository invitationRepository, InvitationEmailService invitationEmailService, InvitationEmailTxService invitationEmailTxService) {
        this.invitationRepository = invitationRepository;
        this.invitationEmailService = invitationEmailService;
        this.invitationEmailTxService = invitationEmailTxService;
    }

    public void process(Long invitationId) {
        Instant now = Instant.now();
        
        boolean locked = invitationEmailTxService.tryMarkSending(invitationId);

        if (!locked) {
            log.warn("Already processed or not found: {}", invitationId);
            return;
        }

        log.info("Marked as SENDING: invitation={}", invitationId);

        try {
            invitationEmailService.sendInvitationEmail(invitationId);
        } catch (Exception e) {
            log.error("Failed to send email: invitaitonId={}", invitationId, e);
            invitationEmailTxService.markFailed(invitationId);
        }
    }
}
