package com.chep.demo.todo.controller.workspace;

import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.dto.workspace.*;
import com.chep.demo.todo.service.workspace.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Tag(name = "Workspace", description = "Workspace 관리 API")
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    @Operation(summary = "워크스페이스 목록", description = "현재 사용자에게 속한 모든 워크스페이스를 반환합니다.")
    @GetMapping
    ResponseEntity<List<WorkspaceResponse>> getWorkspaces() {
        Long userId = currentUserId();
        List<WorkspaceResponse> responses = workspaceService.getMyWorkspaces(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "워크스페이스 생성", description = "새 워크스페이스를 생성하고 현재 사용자를 Owner로 등록합니다.")
    @PostMapping
    ResponseEntity<WorkspaceResponse> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        Long userId = currentUserId();
        Workspace workspace = workspaceService.createWorkspace(userId, request.name(), request.description());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(workspace.getId())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(workspace));
    }

    @Operation(summary = "워크스페이스 수정", description = "워크스페이스 이름과 설명을 수정합니다.")
    @PatchMapping("/{id}")
    ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        Long userId = currentUserId();
        Workspace workspace = workspaceService.updateWorkspace(id, userId, request.name(), request.description());
        return ResponseEntity.ok(toResponse(workspace));
    }

    @Operation(summary = "워크스페이스 상세", description = "워크스페이스 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<WorkspaceResponse> getWorkspace(@PathVariable Long id) {
        Long userId = currentUserId();
        Workspace workspace = workspaceService.getWorkspace(id, userId);
        return ResponseEntity.ok(toResponse(workspace));
    }

    @Operation(summary = "워크스페이스 삭제", description = "워크스페이스를 삭제합니다. Personal Workspace는 삭제할 수 없습니다.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteWorkspace(@PathVariable Long id) {
        Long userId = currentUserId();
        workspaceService.deleteWorkspace(id, userId);
        return ResponseEntity.noContent().build();
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.isPersonal(),
                workspace.getOwner().getId()
        );
    }

    @Operation(summary = "워크스페이스 멤버 목록", description = "해당 워크스페이스의 활성 멤버 목록을 반환합니다.")
    @GetMapping("/{workspaceId}/members")
    ResponseEntity<WorkspaceMemberCursorResponse> getMembers(@PathVariable Long workspaceId,
                                                             @RequestParam(required = false) WorkspaceMember.Status status,
                                                             @RequestParam(required = false) String cursorJoinedAt,
                                                             @RequestParam(required = false) Long cursorMemberId,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(defaultValue = "20") int limit) {
        Long userId = currentUserId();
        Instant cursorInstant = null;
        if (cursorJoinedAt != null) {
            cursorInstant = Instant.parse(cursorJoinedAt);
        }
        WorkspaceMemberCursorResponse responses = workspaceService.getMembers(workspaceId, userId, status, cursorInstant, cursorMemberId, keyword, limit);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "내 멤버십 정보", description = "해당 워크스페이스에서 내 역할과 상태를 조회합니다.")
    @GetMapping("/{workspaceId}/members/me")
    ResponseEntity<WorkspaceMemberResponse> getMyMembership(@PathVariable Long workspaceId) {
        Long userId = currentUserId();
        WorkspaceMember me = workspaceService.getMyMembership(workspaceId, userId);
        return ResponseEntity.ok(toMemberResponse(me));
    }

    @Operation(summary = "멤버 추가", description = "Owner가 새로운 멤버를 추가합니다.")
    @PostMapping("/{workspaceId}/members")
    ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request
    ) {
        Long userId = currentUserId();
        WorkspaceMember member = workspaceService.addMember(workspaceId, userId, request.userId());
        return ResponseEntity.ok(toMemberResponse(member));
    }

    @Operation(summary = "멤버 제거", description = "Owner가 특정 멤버를 제거합니다.")
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    ResponseEntity<Void> removeMember(@PathVariable Long workspaceId, @PathVariable Long workspaceMemberId) {
        Long userId = currentUserId();
        workspaceService.removeMember(workspaceId, userId, workspaceMemberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "멤버 탈퇴", description = "멤버가 스스로 워크스페이스를 떠납니다.")
    @PostMapping("/{workspaceId}/members/leave")
    ResponseEntity<Void> leave(@PathVariable Long workspaceId) {
        Long userId = currentUserId();
        workspaceService.leave(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberResponse(
                workspaceMember.getId(),
                workspaceMember.getUser().getId(),
                workspaceMember.getRole(),
                workspaceMember.getStatus(),
                workspaceMember.getJoinedAt(),
                workspaceMember.getStatusChangedAt()
        );
    }

}
