package com.chep.demo.todo.domain.invitation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByInviteCodeAndSentEmail(String code, String sentEmail);
    List<Invitation> findByInviteCodeWorkspaceIdAndSentEmailAndStatusIn(Long workspaceId, String sentEmail, List<Invitation.Status> statuses);
}
