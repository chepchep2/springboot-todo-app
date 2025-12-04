package com.chep.demo.todo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
// 다른 곳에서 사용 가능하게끔 한다.
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // OncePerRequestFilter: 요청당 한 번만 실행되도록 보장한다.

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    // 오버라이드, 부모의 메서드를 재정의한다.
    protected void doFilterInternal(
            HttpServletRequest request,
            // 클라이언트 요청 정보
            HttpServletResponse response,
            // 클라이언트에게 보낼 응답
            FilterChain filterChain
            // 다음 필터들과의 연결고리
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        // 요청정보에서 Authorization에 해당하는걸 가져온다.

        // 헤더가 없거나 Bearer로 시작하지 않으면 패스
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            // JWT 검증 없이 다음 필터로 넘긴다.
            return;
            // 이 메서드 종료
        }

        String token = authHeader.substring(7);
        // authHeader의 7글자 이후부터 추출("Bearer ")

        // 토큰 유효성 검사 + 현재 SecurityContext 비어 있는지 확인
        if (jwtTokenProvider.validateToken(token)
        && SecurityContextHolder.getContext().getAuthentication() == null) {

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            // 토큰에서 userID 추출

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    // 누구인지
                    null,
                    // 비밀번호(이미 토큰으로 검증했으니 필요 없음)
                    Collections.emptyList()
                    // 권한 목록
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            // SecurityContext에 인증 정보 저장
            // Controller에서 이 userId를 꺼내 쓸 수 있음
        }
        filterChain.doFilter(request, response);
        // 다음 필터, Controller로 넘김
    }

    // 요청 들어옴
    // Authorization 헤더 확인
    // 없으면 -> 그냥 넘김
    // 있으면 -> 토큰 검증
    // 유효하면 -> userId를 SecurityContext에 저장
    // 다음 필터/Controller로 넘김
}
