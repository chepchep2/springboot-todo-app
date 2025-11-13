package com.chep.demo.todo.controller.todo;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.domain.user.UserRepository;
import com.chep.demo.todo.dto.user.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    ResponseEntity<Void> registerUser(
            @Valid @RequestBody RegisterRequest request) {
        var user = new User();
        user.setName(request.name());
        user.setPassword(request.password());
        user.setEmail(request.email());

        var saveUser = userRepository.save(user);

        return ResponseEntity.created()
    }
}
