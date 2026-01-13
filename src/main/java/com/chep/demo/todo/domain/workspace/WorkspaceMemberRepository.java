package com.chep.demo.todo.domain.workspace;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    @Query(value = """
            SELECT wm.*
            FROM workspace_members wm
                     JOIN users u ON wm.user_id = u.id
            WHERE wm.workspace_id = :workspaceId
              AND wm.status = :status
              AND (
                    CAST(:cursorJoinedAt AS timestamptz) IS NULL
                    OR wm.joined_at < CAST(:cursorJoinedAt AS timestamptz)
                    OR (wm.joined_at = CAST(:cursorJoinedAt AS timestamptz) AND wm.id < :cursorMemberId)
                )
              AND (
                    CAST(:keyword AS text) IS NULL
                    OR LOWER(u.name::text) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.email::text) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            ORDER BY wm.joined_at DESC, wm.id DESC
            """, nativeQuery = true)
    List<WorkspaceMember> findMembersWithCursor(@Param("workspaceId") Long workspaceId,
                                                @Param("status") String status,
                                                @Param("cursorJoinedAt") Instant cursorJoinedAt,
                                                @Param("cursorMemberId") Long cursorMemberId,
                                                @Param("keyword") String keyword,
                                                Pageable pageable);


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
