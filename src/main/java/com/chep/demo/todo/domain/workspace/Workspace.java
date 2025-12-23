package com.chep.demo.todo.domain.workspace;

import com.chep.demo.todo.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "workspaces")
@Where(clause = "deleted_at IS NULL")
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workspace_id_gen")
    @SequenceGenerator(name = "workspace_id_gen", sequenceName = "workspace_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "is_personal", nullable = false)
    private boolean personal;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Workspace() {}

    private  Workspace(User owner, String name, String description, boolean personal) {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        this.owner = owner;
        this.name = name;
        this.description = description;
        this.personal = personal;
    }

    public static class Builder {
        private User owner;
        private String name;
        private String description;

        public Builder owner(User owner) {
            this.owner = owner;
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

        public Workspace build() {
            return new Workspace(owner, name, description, false);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Workspace personal(User owner) {
        return new Workspace(owner, "Personal", "Personal Workspace", true);
    }

    public static Workspace of(User owner, String name, String description) {
        return Workspace.builder()
                .owner(owner)
                .name(name)
                .description(description)
                .build();
    }

    public void changeNameAndDescription(String name, String description) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        this.name = name;
        this.description = description;
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }

    public boolean isPersonal() {
        return personal;
    }


    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
