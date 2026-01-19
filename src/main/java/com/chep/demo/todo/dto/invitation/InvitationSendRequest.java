package com.chep.demo.todo.dto.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InvitationSendRequest(
        @NotNull @NotEmpty List<@Email String> emails,
        @Min(1) @Max(30) Integer expiresInDays
) {
}
