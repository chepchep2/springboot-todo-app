package com.chep.demo.todo.controller.auth;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.dto.auth.AuthResponse;
import com.chep.demo.todo.dto.auth.LoginRequest;
import com.chep.demo.todo.dto.auth.RefreshRequest;
import com.chep.demo.todo.dto.auth.RegisterRequest;
import com.chep.demo.todo.service.auth.AuthResult;
import com.chep.demo.todo.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        AuthResult result = authService.register(
                request.email(),
                request.password(),
                request.name()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .replacePath("/api/auth/me")
                .build()
                .toUri();

        return ResponseEntity.created(location).body(toResponse(result));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody LoginRequest request) {

        AuthResult result = authService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getMyInfo() {
        // 1. 토큰에서 userId 꺼내기
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        // 2. DB에서 유저 조회
        User user = authService.getUserById(userId);

        // 3. 응답
        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                null,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshRequest request
            ) {

        AuthResult result  = authService.refresh(request.refreshToken());

        return ResponseEntity.ok(toResponse(result));
    }

    private AuthResponse toResponse(AuthResult result) {
        User user = result.getUser();

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                result.getAccessToken(),
                result.getRefreshToken()
        );
    }
}
