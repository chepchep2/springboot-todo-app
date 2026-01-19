package com.chep.demo.todo.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByWorkspaceId(Long workspaceId);
    Optional<Project> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
