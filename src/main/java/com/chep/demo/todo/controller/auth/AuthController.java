package com.chep.demo.todo.controller.auth;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.dto.auth.AuthResponse;
import com.chep.demo.todo.dto.auth.LoginRequest;
import com.chep.demo.todo.dto.auth.RefreshRequest;
import com.chep.demo.todo.dto.auth.RegisterRequest;
import com.chep.demo.todo.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

        AuthResponse response = authService.register(
                request.email(),
                request.password(),
                request.name()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(response);
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

        AuthResponse response  = authService.refresh(request.refreshToken());

        return ResponseEntity.ok(response);
    }
}
