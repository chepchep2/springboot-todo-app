package com.chep.demo.todo.domain.project;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.workspace.Workspace;
import com.chep.demo.todo.exception.project.ProjectOperationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "projects")
@SQLRestriction("deleted_at IS NULL")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projects_id_gen")
    @SequenceGenerator(name = "projects_id_gen", sequenceName = "project_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Size(max = 120)
    @NotNull
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "is_default", nullable = false)
    private boolean defaultProject;

    protected Project() {}

    private Project(Workspace workspace, User createdBy, String name, String description, boolean defaultProject) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace must not be null");
        }

        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        this.workspace = workspace;
        this.createdBy = createdBy;
        this.name = name;
        this.description = description;
        this.defaultProject = defaultProject;
        this.createdAt = Instant.now();
        this.updatedAt = null;
    }

    public static class Builder {
        private Workspace workspace;
        private User createdBy;
        private String name;
        private String description;
        private boolean defaultProject;

        public Builder workspace(Workspace workspace) {
            this.workspace = workspace;
            return this;
        }

        public Builder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultProject(boolean defaultProject) {
            this.defaultProject = defaultProject;
            return this;
        }

        public Project build() {
            return new Project(workspace, createdBy, name, description, defaultProject);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Project of(Workspace workspace, User createdBy, String name, String description) {
        return Project.builder()
                .workspace(workspace)
                .createdBy(createdBy)
                .name(name)
                .description(description)
                .defaultProject(false)
                .build();
    }

    public static Project defaultProject(Workspace personalWorkspace, User owner) {
        return Project.builder()
                .workspace(personalWorkspace)
                .createdBy(owner)
                .name("Personal Project")
                .description("Default project")
                .defaultProject(true)
                .build();
    }

    public void changeNameAndDescription(String name, String description) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        if (defaultProject) {
            throw new ProjectOperationException("Default project cannot be deleted");
        }
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDefaultProject() {
        return defaultProject;
    }
}
