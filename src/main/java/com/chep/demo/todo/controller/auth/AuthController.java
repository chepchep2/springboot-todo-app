package com.chep.demo.todo.controller.auth;

import com.chep.demo.todo.domain.user.User;
import com.chep.demo.todo.dto.auth.AuthResponse;
import com.chep.demo.todo.dto.auth.LoginRequest;
import com.chep.demo.todo.dto.auth.RefreshRequest;
import com.chep.demo.todo.dto.auth.RegisterRequest;
import com.chep.demo.todo.service.auth.AuthResult;
import com.chep.demo.todo.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Auth", description = "인증 및 회원 관련 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록하고 accessToken, refreshToken을 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 이메일 또는 잘못된 요청")
    })
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

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 accessToken, refreshToken을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })

    @PostMapping("/login")
    ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody LoginRequest request) {

        AuthResult result = authService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(toResponse(result));
    }

    @Operation(
            summary = "내 정보 조회",
            description = "Authorization 헤더의 accessToken을 사용하여 현재 로그인한 사용자의 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료")
    })
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

    @Operation(
            summary = "토큰 재발급",
            description = "유효한 refreshToken으로 새로운 accessToken을 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "refreshToken이 유효하지 않음")
    })
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
