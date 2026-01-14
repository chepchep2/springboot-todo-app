package com.chep.demo.todo.controller.project;

import com.chep.demo.todo.domain.project.Project;
import com.chep.demo.todo.dto.project.CreateProjectRequest;
import com.chep.demo.todo.dto.project.ProjectResponse;
import com.chep.demo.todo.dto.project.UpdateProjectRequest;
import com.chep.demo.todo.service.project.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Project", description = "프로젝트 관리 API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    @Operation(summary = "프로젝트 목록", description = "워크스페이스 내 프로젝트 목록을 반환합니다.")
    @GetMapping
    ResponseEntity<List<ProjectResponse>> getProjects(@PathVariable Long workspaceId) {
        Long userId = currentUserId();
        List<ProjectResponse> responses = projectService.getProjects(workspaceId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "프로젝트 상세", description = "프로젝트 정보를 반환합니다.")
    @GetMapping("/{projectId}")
    ResponseEntity<ProjectResponse> getProject(@PathVariable Long workspaceId, @PathVariable Long projectId) {
        Long userId = currentUserId();
        Project project = projectService.getProject(workspaceId, projectId, userId);
        return ResponseEntity.ok(toResponse(project));
    }

    @Operation(summary = "프로젝트 생성", description = "워크스페이스에 새 프로젝트를 생성합니다.")
    @PostMapping
    ResponseEntity<ProjectResponse> createProject(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        Long userId = currentUserId();
        Project project = projectService.createProject(workspaceId, userId, request.name(), request.description());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(project.getId())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(project));
    }

    @Operation(summary = "프로젝트 수정", description = "프로젝트 이름과 설명을 수정합니다.")
    @PatchMapping("/{projectId}")
    ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long workspaceId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        Long userId = currentUserId();
        Project project = projectService.updateProject(workspaceId, projectId, userId, request.name(), request.description());
        return ResponseEntity.ok(toResponse(project));
    }

    @Operation(summary = "프로젝트 삭제", description = "프로젝트를 삭제합니다. Default Project는 삭제할 수 없습니다.")
    @DeleteMapping("/{projectId}")
    ResponseEntity<Void> deleteProject(@PathVariable Long workspaceId, @PathVariable Long projectId) {
        Long userId = currentUserId();
        projectService.deleteProject(workspaceId, projectId, userId);
        return ResponseEntity.noContent().build();
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getWorkspace().getId(),
                project.getCreatedBy().getId(),
                project.isDefaultProject()
        );
    }
}
