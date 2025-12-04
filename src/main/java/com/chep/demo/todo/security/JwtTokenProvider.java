package com.chep.demo.todo.security;

import com.chep.demo.todo.domain.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
// 다른 클래스에서 사용 가능하다는 걸 표시
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;
    // 지역변수로 SecretKey 타입의 key, long타입의 accessExpirationMillis, refreshExpirationMillis 선언

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-seconds}") long accessExpirationSeconds,
            @Value("${jwt.refresh-expiration-seconds}") long refreshExpirationSeconds
            // 생성자로 application.properties에 있는 각각 만료 시간을 가져온다.
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        // application.properties에 있는 secret을 바이트 배열로 변환
        this.key = Keys.hmacShaKeyFor(keyBytes);
        // 바이트 배열을 hmac-sha 알고리즘용 Secret 객체로 변환
        this.accessExpirationMillis = accessExpirationSeconds * 1000;
        this.refreshExpirationMillis = refreshExpirationSeconds * 1000;
        // 가져온 accessToken와 refreshToken에 * 1000한다. 밀리초로 바꿔주기 위해
    }

    public String generateAccessToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMillis);
        // now 객체 만들어서 현재 시간을 가져오고 거기에 accessToken 만료시간을 더해줘서 expiryDate에 넣는다.

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        // Jwts 내부 메서드인 builder를 써서
        // setSubject(누구 것인지)
        // setIssuedAt(발급 시간)
        // setExpiration(만료시간을 넣고)
        // 비밀키로 서명한다.
        // JWT 문자열로 변환
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMillis);

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                // generateAccessToken이랑 만료시간만 다르다.
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        // 토큰 파싱해서 claims을 가져온다.
        String subject = claims.getSubject();
        // userId 추출

        return Long.parseLong(subject);
        // 문자열을 Long 타입으로 변환
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            // 파싱 시도
            return true;
            // 참(성공)이면 유효한 토큰
        } catch (JwtException | IllegalArgumentException e) {
            // Multi-catch로 간견할게 | 표시 (두 예외를 한 번에 처리)
            // 유효하지 않은 토큰
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                // 서명할 키
                .build()
                .parseClaimsJws(token)
                // 토큰 파싱
                .getBody();
                // 내용 가져오기
    }
}
