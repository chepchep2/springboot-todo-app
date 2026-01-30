package com.chep.demo.todo.service.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;
import com.chep.demo.todo.domain.invitation.InvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Transactional
@Service
public class InvitationEmailTxService {
    private final InvitationRepository invitationRepository;

    public InvitationEmailTxService(
            InvitationRepository invitationRepository
    ) {
        this.invitationRepository = invitationRepository;
    }
    private static final Logger log = LoggerFactory.getLogger(InvitationEmailTxService.class);

    public boolean tryMarkSending(Long invitationId) {
        Instant now = Instant.now();
        int updatedInvitation = invitationRepository.tryMarkSending(invitationId, now);
        return updatedInvitation == 1;
    }

    @Transactional(readOnly = true)
    public Optional<Invitation> getSendingInvitation(Long invitationId) {
        return invitationRepository.findForEmailSend(invitationId)
                .filter(inv -> inv.getStatus() == Invitation.Status.SENDING);
    }

    public void markSent(Long invitationId, Instant sentAt) {
        log.info("markSent called: {}", invitationId);
        Optional<Invitation> optionalInvitation = invitationRepository.findById(invitationId);
        if (optionalInvitation.isEmpty()) {
            return;
        }
        Invitation inv = optionalInvitation.get();
        inv.markSent(sentAt);
    }

    public void markFailed(Long invitationId) {
        Optional<Invitation> optionalInvitation = invitationRepository.findById(invitationId);
        if (optionalInvitation.isEmpty()) {
            return;
        }
        Invitation inv = optionalInvitation.get();
        inv.markFailed();
    }

    public void markCancelled(Long invitationId) {
        log.info("markCancelled called: {}", invitationId);
        Optional<Invitation> optionalInvitation = invitationRepository.findById(invitationId);
        if (optionalInvitation.isEmpty()) {
            return;
        }
        Invitation inv = optionalInvitation.get();
        inv.markCancelled();
    }
}
