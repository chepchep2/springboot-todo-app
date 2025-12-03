package com.chep.demo.todo.domain.todo;

import com.chep.demo.todo.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "todos")
@Where(clause = "deleted_at IS NULL")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todos_id_gen")
    @SequenceGenerator(name = "todos_id_gen", sequenceName = "todo_id_seq", allocationSize = 1)
    private Long id;

    @Size(max = 200)
    @NotNull
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 500)
    @NotNull
    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "due_date")
    private Instant dueDate;

    protected Todo() {}

    private Todo(User user, String title, String content, Integer orderIndex, Instant dueDate) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        if (title == null) {
            throw new IllegalArgumentException("title must not be null");
        }

        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }

        if (orderIndex == null) {
            throw new IllegalArgumentException("orderIndex must not bee null");
        }

        this.user = user;
        this.title =title;
        this.content = content;
        this.orderIndex = orderIndex;
        this.completed = false;
        this.createdAt = Instant.now();
        this.dueDate = dueDate;
        this.updatedAt = null;
    }

    public static class Builder {
        private User user;
        private String title;
        private String content;
        private Integer orderIndex;
        private Instant dueDate;

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder orderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public Builder dueDate(Instant dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Todo build() {
            return new Todo(user, title, content, orderIndex, dueDate);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TodoAssignee> assignees = new HashSet<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void toggleComplete() {
        this.completed = !this.completed;
        this.updatedAt = Instant.now();
    }

    public void changeAssignees(Set<User> users) {
        this.assignees.clear();

        if (users == null || users.isEmpty()) {
            this.updatedAt = Instant.now();
            return;
        }

        for (User user : users) {
            TodoAssignee assignee = TodoAssignee.builder()
                    .todo(this)
                    .user(user)
                    .build();
            this.assignees.add(assignee);
        }
        this.updatedAt = Instant.now();
    }

    public void changeTitleAndContent(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public void changeOrderIndex(Integer orderIndex) {
        if (orderIndex < 0) {
            throw new IllegalArgumentException("orderIndex must be more 0");
        }

        this.orderIndex = orderIndex;
        this.updatedAt = Instant.now();
    }

    public void changeDueDate(Instant dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public User getUser() {
        return user;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public Set<TodoAssignee> getAssignees() {
        return assignees;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
