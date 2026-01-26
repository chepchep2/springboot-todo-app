package com.chep.demo.todo.service.invitation.event;

import com.chep.demo.todo.service.invitation.InvitationEmailAsyncService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvitationEmailEventListener {
    private final InvitationEmailAsyncService invitationEmailAsyncService;

    public InvitationEmailEventListener(InvitationEmailAsyncService invitationEmailAsyncService) {
        this.invitationEmailAsyncService = invitationEmailAsyncService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationsCreated(InvitationsCreatedEvent event) {
        for (Long id : event.invitationIds()) {
            invitationEmailAsyncService.sendInvitationEmail(id);
        }
    }
}
