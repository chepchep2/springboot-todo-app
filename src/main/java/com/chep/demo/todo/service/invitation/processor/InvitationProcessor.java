package com.chep.demo.todo.service.invitation.processor;

import com.chep.demo.todo.domain.invitation.InvitationRepository;
import com.chep.demo.todo.service.invitation.InvitationEmailService;
import com.chep.demo.todo.service.invitation.InvitationEmailTxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InvitationProcessor {
    private static final Logger log = LoggerFactory.getLogger(InvitationProcessor.class);
    private final InvitationEmailService invitationEmailService;
    private final InvitationEmailTxService invitationEmailTxService;

    public InvitationProcessor(InvitationEmailService invitationEmailService, InvitationEmailTxService invitationEmailTxService) {
        this.invitationEmailService = invitationEmailService;
        this.invitationEmailTxService = invitationEmailTxService;
    }

    public void process(Long invitationId) {
        boolean locked = invitationEmailTxService.tryMarkSending(invitationId);

        if (!locked) {
            log.warn("Already processed or not found: {}", invitationId);
            return;
        }

        log.info("Marked as SENDING: invitation={}", invitationId);

        invitationEmailService.sendInvitationEmail(invitationId);
    }
}
