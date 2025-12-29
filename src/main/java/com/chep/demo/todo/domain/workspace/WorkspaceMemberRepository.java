package com.chep.demo.todo.domain.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    @Query("""
            SELECT wm FROM WorkspaceMember wm
            JOIN FETCH wm.workspace w
            WHERE wm.user.id = :userId
              AND wm.status = :status
              AND w.deletedAt IS NULL
            """)
    List<WorkspaceMember> findAllByUserIdAndStatus(@Param("userId") Long userId,
                                                   @Param("status") WorkspaceMember.Status status);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId")
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(@Param("workspaceId") Long workspaceId,
                                                         @Param("userId") Long userId);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId AND wm.status = :status")
    Optional<WorkspaceMember> findByWorkspaceIdAndUserIdAndStatus(@Param("workspaceId") Long workspaceId,
                                                                  @Param("userId") Long userId,
                                                                  @Param("status") WorkspaceMember.Status status);

    List<WorkspaceMember> findAllByWorkspaceId(Long workspaceId);

    List<WorkspaceMember> findAllByWorkspaceIdAndStatus(Long workspaceId, WorkspaceMember.Status status);
}
