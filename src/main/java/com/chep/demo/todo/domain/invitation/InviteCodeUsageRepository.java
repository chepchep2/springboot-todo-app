package com.chep.demo.todo.domain.invitation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteCodeUsageRepository extends JpaRepository<InviteCodeUsage, Long> {
    default void saveIfNotExists(InviteCodeUsage usage) {
        try {
            save(usage);
        } catch (DataIntegrityViolationException e) {
        }
    }
}
