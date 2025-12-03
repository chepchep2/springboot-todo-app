package com.chep.demo.todo.controller.todo;

import com.chep.demo.todo.domain.todo.Todo;
import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.dto.todo.*;
import com.chep.demo.todo.service.todo.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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
    ResponseEntity<List<TodoResponse>> getTodos() {
        Long userId = currentUserId();

        List<TodoResponse> responses = todoService.getTodos(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping
    ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        Long userId = currentUserId();

        Todo created = todoService.createTodo(userId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(created));
    }

    @PutMapping("/{id}")
    ResponseEntity<TodoResponse> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request
    ) {
        Long userId = currentUserId();

        Todo updated = todoService.updateTodo(userId, id, request);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        Long userId = currentUserId();

        todoService.deleteTodo(userId, id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    ResponseEntity<Void> toggleTodoComplete(@PathVariable Long id) {
        Long userId = currentUserId();

        todoService.toggleTodoComplete(userId, id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/move")
    ResponseEntity<Void> moveTodo(
            @PathVariable Long id,
            @Valid @RequestBody MoveTodoRequest request
    ) {
        Long userId = currentUserId();

        todoService.move(userId, id, request);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assignees")
    ResponseEntity<TodoResponse> updateAssignees(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssigneesRequest request
    ) {
        Long userId = currentUserId();
        Todo updated = todoService.updateAssignees(userId, id, request);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PatchMapping("/{id}/due-date")
    ResponseEntity<TodoResponse> updateDueDate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDueDateRequest request
    ) {
        Long userId = currentUserId();
        Todo updated = todoService.updateDueDate(userId, id, request);
        return ResponseEntity.ok(toResponse(updated));
    }

    private TodoResponse toResponse(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContent(),
                todo.isCompleted(),
                todo.getOrderIndex(),
                todo.getDueDate(),
                todo.getAssignees().stream()
                        .map(User::getId)
                        .toList()
        );
    }
}
