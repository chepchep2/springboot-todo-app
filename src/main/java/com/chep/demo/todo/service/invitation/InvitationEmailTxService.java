package com.chep.demo.todo.service.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;
import com.chep.demo.todo.domain.invitation.InvitationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Transactional
@Component
public class InvitationEmailTxService {
    private final InvitationRepository invitationRepository;

    public InvitationEmailTxService(
            InvitationRepository invitationRepository
    ) {
        this.invitationRepository = invitationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Invitation> getPendingInvitation(Long invitationId) {
        Instant now = Instant.now();
        return invitationRepository.findForEmailSend(invitationId)
                .filter(inv -> inv.getStatus() == Invitation.Status.PENDING && !inv.isExpired(now));
    }

    public void markSent(Long invitationId, Instant sentAt) {
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
}
