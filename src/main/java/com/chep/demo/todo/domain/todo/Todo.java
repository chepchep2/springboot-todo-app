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
    @ColumnDefault("now()")
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

    @ManyToMany
    @JoinTable(
            name = "todo_assignees",
            joinColumns = @JoinColumn(name = "todo_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignees = new HashSet<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Set<User> getAssignees() {
        return assignees;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public Integer getOrderIndex() {
        return orderIndex;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void toggleComplete() {
        this.completed = !this.completed;
        this.updatedAt = Instant.now();
    }

    public void changeAssignees(Set<User> assignees) {
        this.assignees.clear();

        if (assignees == null) {
            return;
        }

        this.assignees.addAll(assignees);
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

    public static Todo create(User user, String title, String content, Integer orderIndex, Instant dueDate, Set<User> assignees) {
        Todo todo = new Todo();
        todo.user = user;
        todo.title = title;
        todo.content = content;
        todo.orderIndex = orderIndex;
        todo.dueDate = dueDate;
        todo.completed = false;
        todo.createdAt = Instant.now();

        if (assignees != null) {
            todo.changeAssignees(assignees);
        }

        return todo;
    }
}
