package com.chep.demo.todo.domain.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Query("""
            SELECT DISTINCT w FROM Workspace w
            LEFT JOIN FETCH w.members m
            WHERE w.id = :workspaceId
            """)
    Optional<Workspace> findByIdWithMembers(@Param("workspaceId") Long workspaceId);

    @Query("""
            SELECT DISTINCT w FROM Workspace w
            LEFT JOIN FETCH w.members m
            WHERE m.user.id = :userId
              AND m.status = :status
            """)
    List<Workspace> findAllByMemberUserIdAndStatus(@Param("userId") Long userId,
                                                   @Param("status") WorkspaceMember.Status status);
}
