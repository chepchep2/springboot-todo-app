package com.chep.demo.todo.controller.invitation;

import com.chep.demo.todo.dto.invitation.CreateInvitationRequest;
import com.chep.demo.todo.dto.invitation.InvitationAcceptResult;
import com.chep.demo.todo.dto.invitation.InvitationResult;
import com.chep.demo.todo.dto.invitation.ResendInvitationRequest;
import com.chep.demo.todo.service.invitation.InvitationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/invitations")
public class InvitationController {
    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    @PostMapping
    public InvitationResult create(
            @PathVariable Long workspaceId,
            @RequestBody CreateInvitationRequest request
            ) {
        Long userId = currentUserId();
        return invitationService.createInvitations(
                workspaceId,
                userId,
                request.emails(),
                request.expiresInDays()
        );
    }

    @PostMapping("/resend")
    public InvitationResult resend(
            @PathVariable Long workspaceId,
            @RequestBody ResendInvitationRequest request
    ) {
        Long userId = currentUserId();
        return invitationService.resendInvitation(
                workspaceId,
                userId,
                request.email()
        );
    }

    @PostMapping("/{inviteCode}/accept")
    public InvitationAcceptResult accept(
            @PathVariable Long workspaceId,
            @PathVariable String inviteCode
    ) {
        Long userId = currentUserId();
        return invitationService.acceptInvitation(workspaceId, inviteCode, userId);
    }
}
