package com.chep.demo.todo.domain.todo;

import com.chep.demo.todo.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoReorderTest {
    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .name("tester")
                .email("tester@example.com")
                .password("12345678")
                .build();
    }

    @Test
    void moves_todo_upwards_and_shifts_neighbors_down() {
        Todo target = buildTodo(3);
        Todo first = buildTodo(1);
        Todo second = buildTodo(2);

        List<Todo> changed = Todo.reorder(target, 1, List.of(first, second));

        assertThat(first.getOrderIndex()).isEqualTo(2);
        assertThat(second.getOrderIndex()).isEqualTo(3);
        assertThat(target.getOrderIndex()).isEqualTo(1);
        assertThat(changed).containsExactly(first, second, target);
    }

    @Test
    void moves_todo_downwards_and_shifts_neighbors_up() {
        Todo target = buildTodo(1);
        Todo first = buildTodo(2);
        Todo second = buildTodo(3);

        List<Todo> changed = Todo.reorder(target, 3, List.of(first, second));

        assertThat(first.getOrderIndex()).isEqualTo(1);
        assertThat(second.getOrderIndex()).isEqualTo(2);
        assertThat(target.getOrderIndex()).isEqualTo(3);
        assertThat(changed).containsExactly(first, second, target);
    }

    @Test
    void returns_empty_when_target_index_is_same() {
        Todo target = buildTodo(2);

        List<Todo> changed = Todo.reorder(target, 2, List.of());

        assertThat(changed).isEmpty();
        assertThat(target.getOrderIndex()).isEqualTo(2);
    }

    @Test
    void updates_target_even_when_no_neighbors_exist() {
        Todo target = buildTodo(0);

        List<Todo> changed = Todo.reorder(target, 3, List.of());

        assertThat(changed).containsExactly(target);
        assertThat(target.getOrderIndex()).isEqualTo(3);
    }

    @Test
    void rejects_negative_target_index() {
        Todo target = buildTodo(1);

        assertThatThrownBy(() -> Todo.reorder(target, -1, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetIndex");
    }

    private Todo buildTodo(int orderIndex) {
        return Todo.builder()
                .user(owner)
                .title("todo" + orderIndex)
                .content("content")
                .orderIndex(orderIndex)
                .dueDate(Instant.now())
                .build();
    }
}
