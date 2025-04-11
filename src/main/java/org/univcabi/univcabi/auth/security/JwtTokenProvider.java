package org.univcabi.univcabi.auth.security;


import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.univcabi.univcabi.auth.entity.AuthnRole;

import javax.crypto.SecretKey;
import java.util.*;

@Slf4j
@Component
// JWT 생성 + 파싱 + 검증 담당
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(Arrays.copyOf(secretKey.getBytes(), 32));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

    }

    // JWT 생성 (문자열 반환)
    private String createToken(String studentNumber, AuthnRole role, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        Map<String, Object> claims = new HashMap<>();

        Optional.ofNullable(role).ifPresent(r-> claims.put("role", r.name())); // ADMIN, NORMAL

        return Jwts.builder()
                .subject(studentNumber)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact(); // Header, Payload, Signature 를 묶어서 문자열로 반환
    }

    public String generateAccessToken(String studentNumber, AuthnRole role) {
        return createToken(studentNumber, role, accessTokenExpiration);
    }

    public String generateRefreshToken(String studentNumber) {
        return createToken(studentNumber, null, refreshTokenExpiration);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token); // 해쉬기반 서명 검증 + 만료 검증
            return true;
        } catch (JwtException e){
            log.warn("JWT 유효성 검증 실패: {}",e.getMessage());
            return false;
        }
    }

    public String getStudentNumberFromToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

}