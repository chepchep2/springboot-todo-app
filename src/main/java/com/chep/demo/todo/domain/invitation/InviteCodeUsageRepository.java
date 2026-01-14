package com.chep.demo.todo.domain.invitation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteCodeUsageRepository extends JpaRepository<InviteCodeUsage, Long> {
    Optional<InviteCodeUsage> findByWorkspaceMemberId(Long workspaceMemberId);
}
