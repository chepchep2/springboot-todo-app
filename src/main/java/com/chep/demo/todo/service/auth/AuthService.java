package com.chep.demo.todo.service;

import com.chep.demo.todo.domain.User;
import com.chep.demo.todo.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User signUp(String email, String password, String name) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }

        // User 생성
        var user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(password);

        return userRepository.save(user);
    }
}
