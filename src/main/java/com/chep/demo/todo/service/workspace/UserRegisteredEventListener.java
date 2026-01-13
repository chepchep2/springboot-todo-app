package com.chep.demo.todo.service.workspace;

import com.chep.demo.todo.domain.project.Project;
import com.chep.demo.todo.domain.project.ProjectRepository;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.event.UserRegisteredEvent;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.domain.workspace.WorkspaceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegisteredEventListener {
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;

    public UserRegisteredEventListener(WorkspaceRepository workspaceRepository,
                                       ProjectRepository projectRepository) {
        this.workspaceRepository = workspaceRepository;
        this.projectRepository = projectRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        User owner = event.user();
        Workspace workspace = Workspace.personal(owner);
        Workspace savedWorkspace = workspaceRepository.save(workspace);
        projectRepository.save(Project.defaultProject(savedWorkspace, owner));
    }
}
