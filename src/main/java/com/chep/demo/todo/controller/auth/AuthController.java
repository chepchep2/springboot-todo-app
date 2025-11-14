package com.chep.demo.todo.controller.auth;

import com.chep.demo.todo.dto.auth.AuthResponse;
import com.chep.demo.todo.dto.auth.LoginRequest;
import com.chep.demo.todo.dto.auth.RegisterRequest;
import com.chep.demo.todo.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
