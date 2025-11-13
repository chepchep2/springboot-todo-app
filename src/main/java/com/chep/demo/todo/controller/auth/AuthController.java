package com.chep.demo.todo.controller.auth;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.dto.user.RegisterRequest;
import com.chep.demo.todo.dto.user.RegisterResponse;
import com.chep.demo.todo.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    ResponseEntity<RegisterResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request.email(), request.password(), request.name());

        RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );

        URI location = URI.create("/api/users/" + user.getId());

        return ResponseEntity.created(location).body(response);
    }
}
