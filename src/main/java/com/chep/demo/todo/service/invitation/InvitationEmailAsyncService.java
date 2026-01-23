package com.chep.demo.todo.service.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;
import com.chep.demo.todo.domain.invitation.InvitationRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.service.email.InvitationEmailTemplate;
import com.chep.demo.todo.service.email.ResendEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class InvitationEmailAsyncService {
    private final ResendEmailSender resendEmailSender;
    private final InvitationRepository invitationRepository;

    public InvitationEmailAsyncService(
            ResendEmailSender resendEmailSender,
            InvitationRepository invitationRepository
    ) {
        this.resendEmailSender = resendEmailSender;
        this.invitationRepository = invitationRepository;
    }

    @Transactional
    @Async
    public void sendInvitationEmail(Long invitationId) {
        // 1. invitationId로 Invitation 조회
        Optional<Invitation> invitation = invitationRepository.findForEmailSend(invitationId);

        if (invitation.isEmpty()) {
            return;
        }
        // 1-1. Optional<Invitation> invitation을 꺼낸다.
        Invitation inv = invitation.get();
        // 2. 상태가 PENDING이 아니면 return
        if (inv.getStatus() != Invitation.Status.PENDING) {
            return;
        }
        // 3. inviteUrl 만들기
        String inviteUrl = buildInviteUrl(inv.getInviteCode().getCode());
        // 4. template 만들기
        Workspace workspace = inv.getWorkspace();
        var content = InvitationEmailTemplate.invite(workspace.getName(), inviteUrl);
        // 5. resend 호출, 성공이면 SENT, 실패면 FAILED
        try {
            resendEmailSender.send(inv.getSentEmail(), content.subject(), content.html());

            inv.markSent(Instant.now());
        } catch(Exception e) {
            inv.markFailed();
        }
        invitationRepository.save(inv);
    }

    @Value("${app.base-url}")
    private String baseUrl;
    private String buildInviteUrl(String code) {
        return baseUrl + "/invitations/" + code + "/accept";
    }
}
