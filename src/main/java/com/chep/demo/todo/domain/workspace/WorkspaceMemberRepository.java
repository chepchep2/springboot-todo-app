package com.chep.demo.todo.domain.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    @Query("""
            SELECT LOWER(m.user.email) FROM WorkspaceMember m
            WHERE
                m.workspace.id = :workspaceId
            AND
                m.status = com.chep.demo.todo.domain.workspace.WorkspaceMember.Status.ACTIVE
            """)
    List<String> findActiveMemberEmails(@Param("workspaceId") Long workspaceId);
}
