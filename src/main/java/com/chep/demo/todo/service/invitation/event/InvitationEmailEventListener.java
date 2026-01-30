package com.chep.demo.todo.service.invitation.event;

import com.chep.demo.todo.service.invitation.producer.InvitationQueueProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class InvitationEmailEventListener {
    private final InvitationQueueProducer invitationQueueProducer;

    public InvitationEmailEventListener(InvitationQueueProducer invitationQueueProducer) {
        this.invitationQueueProducer = invitationQueueProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationsCreated(InvitationsCreatedEvent event) {
        for (Long id : event.invitationIds()) {
            invitationQueueProducer.push(id);
        }
    }
}
