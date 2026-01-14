package com.chep.demo.todo.domain.invitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByInviteCodeWorkspaceIdAndSentEmailAndStatusIn(Long workspaceId,
                                                                       String sentEmail,
                                                                       List<Invitation.Status> statuses);

    @Query("""
        SELECT i
        FROM Invitation i
        JOIN FETCH i.inviteCode ic
        JOIN FETCH ic.workspace w
        WHERE ic.code = :code AND i.sentEmail = :sentEmail
    """)
    Optional<Invitation> findWithInviteCodeByCodeAndEmail(@Param("code") String inviteCode,
                                                         @Param("sentEmail") String sentEmail);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Invitation i
        SET i.status = com.chep.demo.todo.domain.invitation.Invitation.Status.EXPIRED,
            i.expiredAt = :expiredAt
        WHERE i.id IN :ids
          AND i.status IN (
            com.chep.demo.todo.domain.invitation.Invitation.Status.PENDING,
            com.chep.demo.todo.domain.invitation.Invitation.Status.SENT
          )
    """)
    int bulkExpirePendingOrSent(@Param("ids") Collection<Long> ids,
                                @Param("expiredAt") Instant expiredAt);
}
