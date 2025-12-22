package com.chep.demo.todo.service.todo;

import com.chep.demo.todo.domain.todo.Todo;
import com.chep.demo.todo.domain.todo.TodoRepository;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.dto.todo.MoveTodoRequest;
import com.chep.demo.todo.exception.todo.TodoNotFoundException;
import com.chep.demo.todo.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoRepository, authService, userRepository);
    }

    @Test
    void move_upwards_shifts_intermediate_items() {
        // current index 3 -> target 1, 중간 구간(1~2)이 +1씩 밀려야 한다.
        Long userId = 1L;
        Long todoId = 10L;
        Todo target = buildTodo(userId, 3);
        Todo affectedOne = buildTodo(userId, 1);
        Todo affectedTwo = buildTodo(userId, 2);

        when(todoRepository.findByIdAndUserId(todoId, userId))
                .thenReturn(Optional.of(target));
        when(todoRepository.findByUserIdAndOrderIndexBetween(userId, 1, 2))
                .thenReturn(List.of(affectedOne, affectedTwo));

        todoService.move(userId, todoId, new MoveTodoRequest(1));

        assertThat(target.getOrderIndex()).isEqualTo(1);
        assertThat(affectedOne.getOrderIndex()).isEqualTo(2);
        assertThat(affectedTwo.getOrderIndex()).isEqualTo(3);
        verify(todoRepository).saveAll(List.of(affectedOne, affectedTwo));
        verify(todoRepository).save(target);
    }

    @Test
    void move_downwards_shifts_intermediate_items() {
        // current index 1 -> target 3, 중간 구간(2~3)이 -1씩 당겨져야 한다.
        Long userId = 1L;
        Long todoId = 20L;
        Todo target = buildTodo(userId, 1);
        Todo affectedOne = buildTodo(userId, 2);
        Todo affectedTwo = buildTodo(userId, 3);

        when(todoRepository.findByIdAndUserId(todoId, userId))
                .thenReturn(Optional.of(target));
        when(todoRepository.findByUserIdAndOrderIndexBetween(userId, 2, 3))
                .thenReturn(List.of(affectedOne, affectedTwo));

        todoService.move(userId, todoId, new MoveTodoRequest(3));

        assertThat(target.getOrderIndex()).isEqualTo(3);
        assertThat(affectedOne.getOrderIndex()).isEqualTo(1);
        assertThat(affectedTwo.getOrderIndex()).isEqualTo(2);
        verify(todoRepository).saveAll(List.of(affectedOne, affectedTwo));
        verify(todoRepository).save(target);
    }

    @Test
    void move_same_index_does_nothing() {
        // target == current이면 어떤 저장도 일어나지 않는다.
        Long userId = 1L;
        Long todoId = 30L;
        Todo target = buildTodo(userId, 2);

        when(todoRepository.findByIdAndUserId(todoId, userId))
                .thenReturn(Optional.of(target));

        todoService.move(userId, todoId, new MoveTodoRequest(2));

        verify(todoRepository, never()).findByUserIdAndOrderIndexBetween(anyLong(), anyInt(), anyInt());
        verify(todoRepository, never()).saveAll(any());
        verify(todoRepository, never()).save(target);
    }

    @Test
    void move_when_todo_not_found_throws_exception() {
        // 조회 결과가 없으면 TodoNotFoundException을 던진다.
        Long userId = 1L;
        Long todoId = 99L;

        when(todoRepository.findByIdAndUserId(todoId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.move(userId, todoId, new MoveTodoRequest(1)))
                .isInstanceOf(TodoNotFoundException.class);

        verify(todoRepository, never()).findByUserIdAndOrderIndexBetween(anyLong(), anyInt(), anyInt());
    }

    private Todo buildTodo(Long userId, int orderIndex) {
        User owner = User.builder()
                .name("user" + userId)
                .email("user" + userId + "@example.com")
                .password("password")
                .build();
        return Todo.builder()
                .user(owner)
                .title("title")
                .content("content")
                .orderIndex(orderIndex)
                .dueDate(Instant.now())
                .build();
    }
}
