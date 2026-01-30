package com.chep.demo.todo.service.invitation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class InvitationLinkBuilder {
    @Value("${app.base-url}")
    private String baseUrl;
    String buildInviteUrl(String code) {
        return baseUrl + "/invitations/" + code + "/accept";
    }
}
