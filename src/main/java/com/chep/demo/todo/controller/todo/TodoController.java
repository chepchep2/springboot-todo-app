package com.chep.demo.todo.controller.todo;

import com.chep.demo.todo.domain.todo.Todo;
import com.chep.demo.todo.domain.todo.TodoRepository;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.dto.todo.CreateTodoRequest;
import com.chep.demo.todo.dto.todo.MoveTodoRequest;
import com.chep.demo.todo.dto.todo.TodoResponse;
import com.chep.demo.todo.dto.todo.UpdateAssigneesRequest;
import com.chep.demo.todo.dto.todo.UpdateTodoRequest;
import com.chep.demo.todo.exception.todo.TodoNotFoundException;
import com.chep.demo.todo.service.auth.AuthService;
import com.chep.demo.todo.service.todo.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    @GetMapping
    List<TodoResponse> getTodos() {
        Long userId = currentUserId();

        return todoService.getTodos(userId);
    }

    @PostMapping
    ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        Long userId = currentUserId();

        return ResponseEntity.ok(todoService.createTodo(userId, request));
    }

    @PutMapping("/{id}")
    ResponseEntity<TodoResponse> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request
    ) {
        Long userId = currentUserId();

        return ResponseEntity.ok(todoService.updateTodo(userId, id, request));
    }

    @DeleteMapping("/{id}")
    void deleteTodo(@PathVariable Long id) {
        Long userId = currentUserId();

        todoService.deleteTodo(userId, id);
    }

    @PatchMapping("/{id}/toggle")
    void toggleTodoComplete(@PathVariable Long id) {
        Long userId = currentUserId();

        todoService.toggleTodoComplete(userId, id);
    }

    @PatchMapping("/{id}/move")
    void moveTodo(
            @PathVariable Long id,
            @Valid @RequestBody MoveTodoRequest request
    ) {
        Long userId = currentUserId();

        todoService.move(userId, id, request);
    }

    @PatchMapping("/{id}/assignees")
    ResponseEntity<TodoResponse> updateAssignees(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssigneesRequest request
    ) {
        Long userId = currentUserId();

        return ResponseEntity.ok(todoService.updateAssignees(userId, id, request));
    }
}
