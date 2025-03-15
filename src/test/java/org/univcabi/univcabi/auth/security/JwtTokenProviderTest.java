package org.univcabi.univcabi.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import lombok.extern.slf4j.Slf4j;


import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp(){
        String secretKey = "thisisexamplejwtsecretkeyjwtsecX";
        long accessTokenExpiration = 1800000;
        long refreshTokenExpiration = 1209600000;

        jwtTokenProvider = new JwtTokenProvider(secretKey,accessTokenExpiration,refreshTokenExpiration);
    }

    @Test  // 생성 및 검증 테스트
    void testGenerateAndValidateAccessToken(){
        String token = jwtTokenProvider.generateAccessToken("202213185","USER");
        assertNotNull(token); // 토큰 null인지 확인

        // JWT 유효성 검사
        assertTrue(jwtTokenProvider.validateToken(token));

        // StudentNumber 추출 테스트
        String studentNumber = jwtTokenProvider.getStudentNumberFromToken(token);
        assertEquals("202213185",studentNumber);
    }

   @Test
    void testExpiredToken(){
        jwtTokenProvider = new JwtTokenProvider("thisisexamplejwtsecretkeyjwtsecX",1000,1000);
        String token = jwtTokenProvider.generateAccessToken("202213185","USER");

        try {
            Thread.sleep(1500);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        // 만료된 토큰 검증 시 실패해야함
        assertFalse(jwtTokenProvider.validateToken(token));

        // 학생 정보 추출시 예외 발생해야함
        assertThrows(ExpiredJwtException.class, ()-> jwtTokenProvider.getStudentNumberFromToken(token));
   }

   @Test
    void testGenerateAndValidRefreshToken(){
       String token = jwtTokenProvider.generateRefreshToken("202213185");
       assertNotNull(token); // 토큰 null인지 확인

       // JWT 유효성 검사
       assertTrue(jwtTokenProvider.validateToken(token));

       // StudentNumber 추출 테스트
       String studentNumber = jwtTokenProvider.getStudentNumberFromToken(token);
       assertEquals("202213185",studentNumber);
   }
}