package com.chep.demo.todo.domain.project;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.workspace.Workspace;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "projects")
@Where(clause = "deleted_at IS NULL")
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

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Project() {}

    private Project(Workspace workspace, User createdBy, String name, String description) {
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
    }

    public static class Builder {
        private Workspace workspace;
        private User createdBy;
        private String name;
        private String description;

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

        public Project build() {
            return new Project(workspace, createdBy, name, description);
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
                .build();
    }

    public static Project defaultProject(Workspace personalWorkspace, User owner) {
        return Project.builder()
                .workspace(personalWorkspace)
                .createdBy(owner)
                .name("Personal Project")
                .description("Default project")
                .build();
    }

    public void changeNameAndDescription(String name, String description) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        this.name = name;
        this.description = description;
    }

    public void archive() {
        this.archivedAt = Instant.now();
    }

    public void unarchive() {
        this.archivedAt = null;
    }

    public void markDeleted() {
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

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
