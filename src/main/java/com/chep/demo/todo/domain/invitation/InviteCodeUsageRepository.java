package com.chep.demo.todo.domain.invitation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteCodeUsageRepository extends JpaRepository<InviteCodeUsage, Long> {
    boolean existsByWorkspaceMemberId(Long workspaceMemberId);
}
