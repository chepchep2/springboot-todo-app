package com.chep.demo.todo.domain.todo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findAllByUserIdOrderByOrderIndexAsc(Long userId);
    Optional<Todo> findByIdAndUserId(Long id, Long userId);
    Long countByUserId(Long userId);
    List<Todo> findByUserIdAndOrderIndexBetween(Long userId, int start, int end);
}
