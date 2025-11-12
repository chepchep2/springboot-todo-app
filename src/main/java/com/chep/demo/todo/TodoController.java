package com.chep.demo.todo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
    private final TodoRepository todoRepository;

    public TodoController(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @GetMapping
    List<Todo> getTodos() {
        return todoRepository.findAll();
    }

    record CreateTodoPayload(
            @NotEmpty(message = "Title is required")
            String title, String content) {}


    @PostMapping
    ResponseEntity<Void> createTodo(
            @Valid @RequestBody CreateTodoPayload payload) {
        var todo = new Todo();
        todo.setTitle(payload.title());
        todo.setContent(payload.content());
        todo.setCompleted(false);
        todo.setCreatedAt(Instant.now());

        var saveTodo = todoRepository.save(todo);

        var url = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .build(saveTodo.getId());

        return ResponseEntity.created(url).build();
    }

    record UpdateTodoPayload(
            @NotEmpty(message = "Title is required")
            String title, String content) {}

    @PutMapping("/{id}")
    ResponseEntity<Void> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoPayload payload
    ) {
        var todo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));
        todo.setTitle(payload.title());
        todo.setContent(payload.content());
        todo.setUpdatedAt(Instant.now());
        todoRepository.save(todo);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    void deleteTodo(@PathVariable Long id) {
        var todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException("Todo not found"));
        todoRepository.delete(todo);
    }

    @ExceptionHandler(TodoNotFoundException.class)
    ResponseEntity<Void> handle(TodoNotFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/toggle")
    ResponseEntity<Void> toggleTodoComplete(@PathVariable Long id) {
        var todo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException("Todo not found"));

        todo.setCompleted(!todo.isCompleted());
        todo.setUpdatedAt(Instant.now());
        todoRepository.save(todo);

        return ResponseEntity.ok().build();
    }


}
