package org.univcabi.univcabi.auth.security;


import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;


import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    private String createToken(String studentNumber, String role, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        Claims claims = Jwts.claims().issuedAt(now).subject(studentNumber).build();
        if (role != null) {
            claims.put("role", role);
        }

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateAccessToken(String studentNumber, String role) {
        return createToken(studentNumber, role, accessTokenExpiration);
    }

    public String generateRefreshToken(String studentNumber) {
        return createToken(studentNumber, null, refreshTokenExpiration);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT: {}", e.getMessage());
        }
        return false;
    }

    public String getStudentNumberFromToken(String token) {
        return Jwts.parser()
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Date getExpirationDate(String token) {
        return Jwts.parser()
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }
}