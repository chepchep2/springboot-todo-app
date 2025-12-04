package com.chep.demo.todo.controller.todo;

import com.chep.demo.todo.domain.todo.Todo;
import com.chep.demo.todo.dto.todo.*;
import com.chep.demo.todo.service.todo.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Todo", description = "Todo 관리 API")
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

    @Operation(
            summary = "Todo 목록 조회",
            description = "현재 로그인한 사용자의 Todo 목록을 orderIndex 오름차순으로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    ResponseEntity<List<TodoResponse>> getTodos() {
        Long userId = currentUserId();

        List<TodoResponse> responses = todoService.getTodos(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Todo 생성",
            description = "새로운 Todo를 생성합니다. orderIndex가 null이면 자동으로 마지막 순서에 배치됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 또는 잘못된 assigneeId 포함")
    })
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

    @Operation(
            summary = "Todo 수정",
            description = "Todo의 제목(title)과 내용(content)을 수정합니다. 순서(orderIndex)는 이 API에서 수정하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 권한이 없는 Todo ID")
    })
    @PutMapping("/{id}")
    ResponseEntity<TodoResponse> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request
    ) {
        Long userId = currentUserId();

        Todo updated = todoService.updateTodo(userId, id, request);
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(
            summary = "Todo 삭제",
            description = "Todo를 soft delete 방식으로 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 권한이 없는 Todo ID")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        Long userId = currentUserId();

        todoService.deleteTodo(userId, id);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Todo 완료 토글",
            description = "Todo의 completed 상태를 true/false로 토글합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "토글 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 권한이 없는 Todo ID")
    })
    @PatchMapping("/{id}/toggle")
    ResponseEntity<Void> toggleTodoComplete(@PathVariable Long id) {
        Long userId = currentUserId();

        todoService.toggleTodoComplete(userId, id);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Todo 순서 이동",
            description = "Todo의 orderIndex를 재배치합니다. 중간에 있는 Todo들의 orderIndex는 자동 조정됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "순서 변경 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 권한이 없는 Todo ID")
    })
    @PatchMapping("/{id}/move")
    ResponseEntity<Void> moveTodo(
            @PathVariable Long id,
            @Valid @RequestBody MoveTodoRequest request
    ) {
        Long userId = currentUserId();

        todoService.move(userId, id, request);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "담당자 목록 변경",
            description = "Todo의 assigneeIds 전체를 새로운 목록으로 교체합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 assigneeId 포함"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 권한이 없는 Todo ID")
    })
    @PatchMapping("/{id}/assignees")
    ResponseEntity<TodoResponse> updateAssignees(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssigneesRequest request
    ) {
        Long userId = currentUserId();
        Todo updated = todoService.updateAssignees(userId, id, request);
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(
            summary = "Todo 마감일 변경",
            description = "Todo의 dueDate 값을 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 권한이 없는 Todo ID")
    })
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
                        .map(assignee -> assignee.getUser().getId())
                        .toList()
        );
    }
}
