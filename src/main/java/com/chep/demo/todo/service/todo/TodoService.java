package com.chep.demo.todo.service.todo;

import com.chep.demo.todo.domain.todo.Todo;
import com.chep.demo.todo.domain.todo.TodoRepository;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.dto.todo.CreateTodoRequest;
import com.chep.demo.todo.dto.todo.MoveTodoRequest;
import com.chep.demo.todo.dto.todo.UpdateAssigneesRequest;
import com.chep.demo.todo.dto.todo.UpdateDueDateRequest;
import com.chep.demo.todo.dto.todo.UpdateTodoRequest;
import com.chep.demo.todo.exception.auth.AuthenticationException;
import com.chep.demo.todo.exception.todo.TodoNotFoundException;
import com.chep.demo.todo.service.auth.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
@Transactional
public class TodoService {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public TodoService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Todo> getTodos(Long userId) {
        return todoRepository.findAllByUserIdOrderByOrderIndexAsc(userId);
    }

    public Todo createTodo(Long userId, CreateTodoRequest request) {
        Integer orderIndex = request.orderIndex();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        Long totalCounting = todoRepository.countByUserId(userId);
        int totalCount = totalCounting.intValue();

        if (orderIndex == null) {
            orderIndex = totalCount;
        } else {
            // 0 ~ totalCount 사이만 허용
            if (orderIndex < 0 || orderIndex > totalCount) {
                throw new IllegalArgumentException("orderIndex out of range: 0 ~ " + totalCount);
            }

            if (orderIndex < totalCount) {
                List<Todo> affectedTodos = todoRepository.findByUserIdAndOrderIndexBetween(userId, orderIndex, totalCount - 1);

                shiftOrderIndexRange(affectedTodos, + 1);

                todoRepository.saveAll(affectedTodos);
            }
        }

        Set<User> assignees = resolveAssignees(request.assigneeIds());

        Todo todo = Todo.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .orderIndex(orderIndex)
                .dueDate(request.dueDate())
                .build();

        todo.changeAssignees(assignees);

        return todoRepository.save(todo);
    }

    private void shiftOrderIndexRange(List<Todo> affectedTodos, int delta) {
        for (Todo affected: affectedTodos) {
            affected.changeOrderIndex(affected.getOrderIndex() + delta);
        }
    }

    private Set<User> resolveAssignees(List<Long> assigneeIds) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return new HashSet<>();
        }

        List<User> users = userRepository.findAllById(assigneeIds);
        if (users.size() != new HashSet<>(assigneeIds).size()) {
            throw new IllegalArgumentException("Invalid assignee id provided");
        }
        return new HashSet<>(users);
    }

    public Todo updateTodo(Long userId, Long todoId, UpdateTodoRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        todo.changeTitleAndContent(request.title(), request.content());

        return todoRepository.save(todo);
    }

    public void deleteTodo(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        int deletedOrderIndex = todo.getOrderIndex();

        todoRepository.softDelete(todo);

        List<Todo> affectedTodos = todoRepository.findByUserIdAndOrderIndexGreaterThan(userId, deletedOrderIndex);

        shiftOrderIndexRange(affectedTodos, - 1);

        todoRepository.saveAll(affectedTodos);
    }

    public void toggleTodoComplete(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));
        todo.toggleComplete();

        todoRepository.save(todo);
    }

    public void move(Long userId, Long todoId, MoveTodoRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        Integer targetOrderIndex = request.targetOrderIndex();
        Integer currentOrderIndex = todo.getOrderIndex();

        if (targetOrderIndex.equals(currentOrderIndex)) {
            return;
        }

        Long totalCountLong = todoRepository.countByUserId(userId);
        int totalCount = totalCountLong.intValue();

        if (targetOrderIndex < 0 || targetOrderIndex >= totalCount) {
            throw new IllegalArgumentException("targetOrderIndex out of range: 0 ~ " + (totalCount - 1));
        }

        int start;
        int end;
        int delta;

        if (targetOrderIndex < currentOrderIndex) {
            start = targetOrderIndex;
            end = currentOrderIndex - 1;
            delta = 1;
        } else {
            start = currentOrderIndex + 1;
            end = targetOrderIndex;
            delta = -1;
        }

        List<Todo> affectedTodos = todoRepository.findByUserIdAndOrderIndexBetween(userId, start, end);

        shiftOrderIndexRange(affectedTodos, delta);

        todo.changeOrderIndex(targetOrderIndex);

        todoRepository.saveAll(affectedTodos);
        todoRepository.save(todo);
    }

    public Todo updateAssignees(Long userId, Long todoId, UpdateAssigneesRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        todo.changeAssignees(resolveAssignees(request.assigneeIds()));
        return  todoRepository.save(todo);
    }

    public Todo updateDueDate(Long userId, Long todoId, UpdateDueDateRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        todo.changeDueDate((request.dueDate()));
        return todoRepository.save(todo);
    }
}
