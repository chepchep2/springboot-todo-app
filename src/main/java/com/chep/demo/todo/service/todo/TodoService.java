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

    public TodoService(TodoRepository todoRepository, AuthService authService, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Todo> getTodos(Long userId) {
        return todoRepository.findAllByUserIdOrderByOrderIndexAsc(userId);
    }

    public Todo createTodo(Long userId, CreateTodoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        Integer orderIndex = request.orderIndex();
        if (orderIndex == null) {
            orderIndex = todoRepository.countByUserId(userId).intValue();
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

        todoRepository.softDelete(todo);
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

        List<Todo> affectedTodos = todoRepository.findByUserIdAndOrderIndexBetween(
                userId,
                start,
                end
        );

        for (Todo affected : affectedTodos) {
            affected.changeOrderIndex(affected.getOrderIndex() + delta);
        }

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
