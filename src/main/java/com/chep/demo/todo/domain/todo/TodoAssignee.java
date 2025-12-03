package com.chep.demo.todo.domain.todo;

import com.chep.demo.todo.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "todo_assignees",
uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_todo_assignee_todo_user",
                columnNames = {"todo_id", "user_id"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder
    private TodoAssignee(User user, Todo todo) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (todo == null) {
            throw new IllegalArgumentException("todo must not be null");
        }

        this.todo = todo;
        this.user = user;
        this.createdAt = Instant.now();
    }
}