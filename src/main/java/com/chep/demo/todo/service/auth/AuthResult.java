package com.chep.demo.todo.service.auth;

import com.chep.demo.todo.domain.user.User;

public class AuthResult {
    private final User user;
    private final String accessToken;
    private final String refreshToken;

    public AuthResult(User user, String accessToken, String refreshToken) {
        this.user = user;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public User getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
