package com.chep.demo.todo.dto.invitation;

import java.util.List;

public record CreateInvitationRequest(List<String> emails,
                                      Integer expiresInDays) {
}
