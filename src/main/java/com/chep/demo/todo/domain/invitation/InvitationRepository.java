package com.chep.demo.todo.domain.invitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByInviteCodeCodeAndSentEmail(String code, String sentEmail);
    List<Invitation> findByInviteCodeWorkspaceIdAndSentEmailAndStatusIn(Long workspaceId, String sentEmail, List<Invitation.Status> statuses);
    @Query("""
            SELECT i
            FROM Invitation i
            JOIN FETCH i.inviteCode ic
            JOIN FETCH ic.workspace w
            WHERE i.id = :invitationId
            """)
    Optional<Invitation> findForEmailSend(@Param("invitationId") Long invitationId);
}
