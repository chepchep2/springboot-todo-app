package com.chep.demo.todo.domain.workspace;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    @Query("""
        SELECT wm FROM WorkspaceMember wm
        JOIN FETCH wm.user u
        WHERE wm.workspace.id = :workspaceId
            AND wm.status = :status
        ORDER BY wm.joinedAt DESC, wm.id DESC
        """)
    List<WorkspaceMember> findFirstPage(
            @Param("workspaceId") Long workspaceId,
            @Param("status") WorkspaceMember.Status status,
            Pageable pageable
    );

    @Query("""
            SELECT wm FROM WorkspaceMember wm
            JOIN FETCH wm.user u
            WHERE wm.workspace.id = :workspaceId
                AND wm.status = :status
                AND (
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            ORDER BY wm.joinedAt DESC, wm.id DESC
            """)
    List<WorkspaceMember> findFirstPageWithKeyword(
            @Param("workspaceId") Long workspaceId,
            @Param("status") WorkspaceMember.Status status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
        SELECT wm FROM WorkspaceMember wm
        JOIN FETCH wm.user u
        WHERE wm.workspace.id = :workspaceId
            AND wm.status = :status
            AND (
                wm.joinedAt < :cursorJoinedAt
                OR (wm.joinedAt = :cursorJoinedAt AND wm.id < :cursorMemberId)
            )
        ORDER BY wm.joinedAt DESC, wm.id DESC
        """)
    List<WorkspaceMember> findNextPage(
            @Param("workspaceId") Long workspaceId,
            @Param("status") WorkspaceMember.Status status,
            @Param("cursorJoinedAt") Instant cursorJoinedAt,
            @Param("cursorMemberId") Long cursorMemberId,
            Pageable pageable
    );

    @Query("""
    SELECT wm FROM WorkspaceMember wm
    JOIN FETCH wm.user u
    WHERE wm.workspace.id = :workspaceId
        AND wm.status = :status
        AND (
            wm.joinedAt < :cursorJoinedAt
            OR (wm.joinedAt = :cursorJoinedAt AND wm.id < :cursorMemberId)
        )
        AND (
            LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    ORDER BY wm.joinedAt DESC, wm.id DESC
    """)
    List<WorkspaceMember> findNextPageWithKeyword(
            @Param("workspaceId") Long workspaceId,
            @Param("status") WorkspaceMember.Status status,
            @Param("cursorJoinedAt") Instant cursorJoinedAt,
            @Param("cursorMemberId") Long cursorMemberId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT LOWER(m.user.email) FROM WorkspaceMember m
            WHERE
                m.workspace.id = :workspaceId
            AND
                m.status = com.chep.demo.todo.domain.workspace.WorkspaceMember.Status.ACTIVE
            """)
    List<String> findActiveMemberEmails(@Param("workspaceId") Long workspaceId);
}
