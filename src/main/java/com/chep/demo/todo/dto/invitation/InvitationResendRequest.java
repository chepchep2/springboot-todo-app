package com.chep.demo.todo.dto.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InvitationResendRequest(@NotBlank @Email String email) {
}
