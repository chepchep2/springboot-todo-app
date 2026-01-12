package com.chep.demo.todo.service.project;

import com.chep.demo.todo.domain.project.Project;
import com.chep.demo.todo.domain.project.ProjectRepository;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceMember;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import com.chep.demo.todo.exception.project.ProjectNotFoundException;
import com.chep.demo.todo.exception.workspace.WorkspaceAccessDeniedException;
import com.chep.demo.todo.exception.workspace.WorkspaceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;

    public ProjectService(ProjectRepository projectRepository,
                          WorkspaceRepository workspaceRepository) {
        this.projectRepository = projectRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<Project> getProjects(Long workspaceId, Long userId) {
        loadWorkspaceMember(workspaceId, userId);
        return projectRepository.findAllByWorkspaceId(workspaceId);
    }

    public Project createProject(Long workspaceId, Long userId, String name, String description) {
        WorkspaceMember member = loadWorkspaceMember(workspaceId, userId);
        Project project = Project.of(member.getWorkspace(), member.getUser(), name, description);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Project getProject(Long workspaceId, Long projectId, Long userId) {
        loadWorkspaceMember(workspaceId, userId);
        return findProject(workspaceId, projectId);
    }

    public Project updateProject(Long workspaceId, Long projectId, Long userId, String name, String description) {
        WorkspaceMember member = loadWorkspaceMember(workspaceId, userId);
        Project project = findProject(workspaceId, projectId);
        ensureCanModifyProject(member, project);
        project.changeNameAndDescription(name, description);
        return projectRepository.save(project);
    }

    public void deleteProject(Long workspaceId, Long projectId, Long userId) {
        WorkspaceMember member = loadWorkspaceMember(workspaceId, userId);
        Project project = findProject(workspaceId, projectId);
        ensureCanModifyProject(member, project);
        project.markDeleted();
        projectRepository.save(project);
    }

    private WorkspaceMember loadWorkspaceMember(Long workspaceId, Long userId) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found."));

        return workspace.requireActiveMember(userId);
    }

    private Project findProject(Long workspaceId, Long projectId) {
        return projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));
    }

    private void ensureCanModifyProject(WorkspaceMember member, Project project) {
        boolean isOwner = member.getRole() == WorkspaceMember.Role.OWNER;
        boolean isCreator = project.getCreatedBy().getId().equals(member.getUser().getId());
        if (!isOwner && !isCreator) {
            throw new WorkspaceAccessDeniedException("You do not have permission to modify this project.");
        }
    }
}
