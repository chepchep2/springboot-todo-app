package com.chep.demo.todo.service.todo;

import com.chep.demo.todo.domain.todo.Todo;
import com.chep.demo.todo.domain.todo.TodoRepository;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.dto.todo.CreateTodoRequest;
import com.chep.demo.todo.dto.todo.MoveTodoRequest;
import com.chep.demo.todo.dto.todo.TodoResponse;
import com.chep.demo.todo.dto.todo.UpdateAssigneesRequest;
import com.chep.demo.todo.dto.todo.UpdateDueDateRequest;
import com.chep.demo.todo.dto.todo.UpdateTodoRequest;
import com.chep.demo.todo.exception.todo.TodoNotFoundException;
import com.chep.demo.todo.service.auth.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
@Transactional
public class TodoService {
    private final TodoRepository todoRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    public TodoService(TodoRepository todoRepository, AuthService authService, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getTodos(Long userId) {
        return todoRepository.findAllByUserIdOrderByOrderIndexAsc(userId)
                .stream()
                .map(todo -> new TodoResponse(
                        todo.getId(),
                        todo.getTitle(),
                        todo.getContent(),
                        todo.isCompleted(),
                        todo.getOrderIndex(),
                        todo.getDueDate(),
                        assigneeIds(todo)
                )).toList();
    }

    public TodoResponse createTodo(Long userId, CreateTodoRequest request) {
        User user = authService.getUserById(userId);

        Integer orderIndex = request.orderIndex();
        if (orderIndex == null) {
            orderIndex = todoRepository.countByUserId(userId).intValue();
        }

        Set<User> assignees = resolveAssignees(request.assigneeIds());

        Todo todo = new Todo();
        todo.setTitle(request.title());
        todo.setContent(request.content());
        todo.setCompleted(false);
        todo.setCreatedAt(Instant.now());
        todo.setOrderIndex(orderIndex);
        todo.setUser(user);
        todo.setDueDate(request.dueDate());
        todo.setAssignees(assignees);

        Todo saved = todoRepository.save(todo);

        return new TodoResponse(saved.getId(),
                saved.getTitle(),
                saved.getContent(),
                saved.isCompleted(),
                saved.getOrderIndex(),
                saved.getDueDate(),
                assigneeIds(saved)
        );
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

    private List<Long> assigneeIds(Todo todo) {
        return todo.getAssignees()
                .stream()
                .map(user -> user.getId())
                .toList();
    }

    public TodoResponse updateTodo(Long userId, Long todoId, UpdateTodoRequest request) {
        User user = authService.getUserById(userId);

        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));
        todo.setTitle(request.title());
        todo.setContent(request.content());
        todo.setUpdatedAt(Instant.now());
        todo.setUser(user);

        if (request.orderIndex() != null) {
            todo.setOrderIndex(request.orderIndex());
        }

        Todo updatedTodo = todoRepository.save(todo);

        return new TodoResponse(
                updatedTodo.getId(),
                updatedTodo.getTitle(),
                updatedTodo.getContent(),
                updatedTodo.isCompleted(),
                updatedTodo.getOrderIndex(),
                updatedTodo.getDueDate(),
                assigneeIds(updatedTodo)
        );
    }

    public void deleteTodo(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));
        todoRepository.delete(todo);
    }

    public void toggleTodoComplete(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));
        todo.setCompleted(!todo.isCompleted());
        todo.setUpdatedAt(Instant.now());
        todoRepository.save(todo);
    }

    public void move(Long userId, Long todoId, MoveTodoRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        Integer targetOrderIndex = request.targetOrderIndex();
        Integer currentOrderIndex = todo.getOrderIndex();

        if (targetOrderIndex < 0) {
            throw new IllegalArgumentException("targetOrderIndex mut be more 0");
        }

        if (targetOrderIndex.equals(currentOrderIndex)) {
            return;
        }

        List<Todo> affectedTodos;

        if (targetOrderIndex < currentOrderIndex) {
            affectedTodos = todoRepository.findByUserIdAndOrderIndexBetween(
                    userId,
                    targetOrderIndex,
                    currentOrderIndex -1
            );

            for (Todo affected: affectedTodos) {
                affected.setOrderIndex(affected.getOrderIndex() + 1);
            }
        } else {
            affectedTodos = todoRepository.findByUserIdAndOrderIndexBetween(
                    userId,
                    currentOrderIndex + 1,
                    targetOrderIndex
            );

            for (Todo affected: affectedTodos) {
                affected.setOrderIndex(affected.getOrderIndex() - 1);
            }
        }

        todoRepository.saveAll(affectedTodos);
        todo.setOrderIndex(targetOrderIndex);
        todoRepository.save(todo);
    }

    public TodoResponse updateAssignees(Long userId, Long todoId, UpdateAssigneesRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        todo.setAssignees(resolveAssignees(request.assigneeIds()));
        Todo updated = todoRepository.save(todo);

        return new TodoResponse(
                updated.getId(),
                updated.getTitle(),
                updated.getContent(),
                updated.isCompleted(),
                updated.getOrderIndex(),
                updated.getDueDate(),
                assigneeIds(updated)
        );
    }

    public TodoResponse updateDueDate(Long userId, Long todoId, UpdateDueDateRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        todo.setDueDate((request.dueDate()));
        Todo updated = todoRepository.save(todo);

        return new TodoResponse(
                updated.getId(),
                updated.getTitle(),
                updated.getContent(),
                updated.isCompleted(),
                updated.getOrderIndex(),
                updated.getDueDate(),
                assigneeIds(updated)
        );
    }
}
