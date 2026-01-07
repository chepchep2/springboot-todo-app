package com.chep.demo.todo.controller.workspace;

import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.dto.workspace.AddWorkspaceMemberRequest;
import com.chep.demo.todo.dto.workspace.WorkspaceMemberPage;
import com.chep.demo.todo.dto.workspace.WorkspaceMemberPageResponse;
import com.chep.demo.todo.dto.workspace.WorkspaceMemberResponse;
import com.chep.demo.todo.service.workspace.WorkspaceMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "WorkspaceMember", description = "워크스페이스 멤버 관리 API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;

    public WorkspaceMemberController(WorkspaceMemberService workspaceMemberService) {
        this.workspaceMemberService = workspaceMemberService;
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    @Operation(summary = "멤버 목록", description = "워크스페이스 멤버 목록을 반환합니다.")
    @GetMapping
    ResponseEntity<List<WorkspaceMemberResponse>> getMembers(@PathVariable Long workspaceId) {
        Long userId = currentUserId();
        List<WorkspaceMemberResponse> responses = workspaceMemberService.getMembers(workspaceId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/cursor")
    ResponseEntity<WorkspaceMemberPageResponse> getMembersWithCursor(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) WorkspaceMember.Status status,
            @RequestParam(required = false) String cursorJoinedAt,
            @RequestParam(required = false) Long cursorMemberId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "20") int limit
            ) {
        Long userId = currentUserId();

        WorkspaceMemberPage page = workspaceMemberService.getMembersWithCursor(
                workspaceId, userId, status, cursorJoinedAt, cursorMemberId, keyword, limit
        );

        List<WorkspaceMemberResponse> responses = page.members().stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new WorkspaceMemberPageResponse(
                responses,
                page.hasNext(),
                page.nextCursorJoinedAt(),
                page.nextCursorMemberId()
        ));
    }

    @Operation(summary = "내 멤버 정보", description = "해당 워크스페이스에서 내 역할/상태를 조회합니다.")
    @GetMapping("/me")
    ResponseEntity<WorkspaceMemberResponse> getMyMembership(@PathVariable Long workspaceId) {
        Long userId = currentUserId();
        WorkspaceMember me = workspaceMemberService.getMyMembership(workspaceId, userId);
        return ResponseEntity.ok(toResponse(me));
    }

    @Operation(summary = "멤버 추가", description = "OWNER가 새로운 멤버를 초대/추가합니다.")
    @PostMapping
    ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request
    ) {
        Long userId = currentUserId();
        WorkspaceMember member = workspaceMemberService.addMember(workspaceId, userId, request.userId());
        return ResponseEntity.ok(toResponse(member));
    }

    @Operation(summary = "멤버 제거", description = "OWNER가 특정 멤버를 제거합니다.")
    @DeleteMapping("/{memberId}")
    ResponseEntity<Void> removeMember(@PathVariable Long workspaceId, @PathVariable Long memberId) {
        Long userId = currentUserId();
        workspaceMemberService.removeMember(workspaceId, userId, memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "자발적 탈퇴", description = "멤버가 스스로 워크스페이스에서 나갑니다.")
    @PostMapping("/leave")
    ResponseEntity<Void> leave(@PathVariable Long workspaceId) {
        Long userId = currentUserId();
        workspaceMemberService.leave(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }

    private WorkspaceMemberResponse toResponse(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getStatusChangedAt()
        );
    }
}
