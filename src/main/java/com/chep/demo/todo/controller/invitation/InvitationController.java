package com.chep.demo.todo.controller.invitation;

import com.chep.demo.todo.domain.invitation.Invitation;
import com.chep.demo.todo.dto.invitation.InvitationAcceptResponse;
import com.chep.demo.todo.dto.invitation.InvitationAcceptResult;
import com.chep.demo.todo.dto.invitation.InvitationSendRequest;
import com.chep.demo.todo.dto.invitation.InvitationSummaryDto;
import com.chep.demo.todo.dto.invitation.InvitationResendRequest;
import com.chep.demo.todo.dto.invitation.InvitationSendResponse;
import com.chep.demo.todo.dto.invitation.InvitationSendResult;
import com.chep.demo.todo.service.invitation.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Invitation", description = "워크스페이스 초대 API")
@RestController
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

    @Operation(summary = "워크스페이스 초대 생성")
    @PostMapping("/api/workspaces/{workspaceId}/invitations")
    public ResponseEntity<InvitationSendResponse> sendInvitations(
            @PathVariable Long workspaceId,
            @Valid @RequestBody InvitationSendRequest request
    ) {
        Long userId = currentUserId();
        InvitationSendResult result = invitationService.sendInvitations(
                workspaceId,
                userId,
                request.emails(),
                request.expiresInDays()
        );
        return ResponseEntity.ok(toSendResponse(result));
    }

    @Operation(summary = "워크스페이스 초대 재발송")
    @PostMapping("/api/workspaces/{workspaceId}/invitations/resend")
    public ResponseEntity<InvitationSendResponse> resendInvitation(
            @PathVariable Long workspaceId,
            @Valid @RequestBody InvitationResendRequest request
    ) {
        Long userId = currentUserId();
        InvitationSendResult result = invitationService.resendInvitation(
                workspaceId,
                userId,
                request.email()
        );
        return ResponseEntity.ok(toSendResponse(result));
    }

    @Operation(summary = "초대 수락")
    @PostMapping("/api/invitations/{inviteCode}/accept")
    public ResponseEntity<InvitationAcceptResponse> acceptInvitation(@PathVariable String inviteCode) {
        Long userId = currentUserId();
        InvitationAcceptResult result = invitationService.acceptInvitation(inviteCode, userId);
        return ResponseEntity.ok(InvitationAcceptResponse.from(result));
    }

    private InvitationSendResponse toSendResponse(InvitationSendResult result) {
        if (!result.hasInvitations()) {
            return new InvitationSendResponse(null, null, List.of());
        }

        List<InvitationSummaryDto> summaries = new ArrayList<>();
        for (Invitation invitation : result.invitations()) {
            summaries.add(InvitationSummaryDto.from(invitation));
        }

        return new InvitationSendResponse(
                result.inviteCode().getCode(),
                result.inviteCode().getExpiresAt(),
                summaries
        );
    }
}
