package com.chep.demo.todo.domain.todo;

import com.chep.demo.todo.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "todo_assignees",
uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_todo_assignee_todo_user",
                columnNames = {"todo_id", "user_id"}
        )
    }
)
public class TodoAssignee {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todo_assignees_id")
    @SequenceGenerator(name = "todo_assignees_id", sequenceName = "todo_assignee_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TodoAssignee() {}

    private TodoAssignee(Todo todo, User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (todo == null) {
            throw new IllegalArgumentException("todo must not be null");
        }

        this.user =user;
        this.todo = todo;
        this.createdAt = Instant.now();
    }

    public static class Builder {
        private User user;
        private Todo todo;

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder todo(Todo todo) {
            this.todo = todo;
            return this;
        }

        public TodoAssignee build() {
            return new TodoAssignee(todo, user);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public Todo getTodo() {
        return todo;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}