package org.univcabi.univcabi.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import org.univcabi.univcabi.auth.service.TokenService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 특정 경로는 인증 필터에서 제외 (예: 로그인, 회원가입)
        if (shouldSkipFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = resolveToken(request);

            if (token != null) {
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        authenticateUser(token);
                    }
                } catch (ExpiredJwtException e) {
                    log.warn("AccessToken이 만료됨. RefreshToken으로 재발급 시도");

                    try {
                        String refreshToken = tokenService.getRefreshTokenFromCookie(request);
                        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                            String studentNumber = jwtTokenProvider.getStudentNumberFromToken(refreshToken);
                            String storedRefreshToken = tokenService.getRefreshToken(studentNumber);

                            if (refreshToken.equals(storedRefreshToken)) {
                                String newAccessToken = jwtTokenProvider.generateAccessToken(studentNumber, "USER");
                                response.setHeader("Authorization", "Bearer " + newAccessToken);
                                log.info("새로운 AccessToken 발급 및 쿠키 저장 완료");
                                authenticateUser(newAccessToken);
                            } else {
                                log.error("RefreshToken이 유효하지 않음. 재발급 불가");
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh Token이 유효하지 않음");
                                return;
                            }
                        } else {
                            log.error("RefreshToken이 존재하지 않거나 유효하지 않음.");
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh Token이 유효하지 않음");
                            return;
                        }
                    } catch (Exception refreshException) {
                        log.error("RefreshToken 처리 중 오류 발생: {}", refreshException.getMessage());
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 처리 중 오류 발생");
                        return;
                    }
                } catch (Exception e) {
                    log.error("Token 처리 중 예외 발생: {}", e.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 처리 중 오류 발생");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 필터 처리 중 발생한 예외는 여기서 잡아서 원래 예외를 유지
            log.error("JWT 필터 처리 중 예외 발생: {}", e.getMessage(), e);
            // 이미 응답이 커밋되지 않았다면 에러 전송
            if (!response.isCommitted()) {
                // 인증 관련 예외가 아닌 경우 500 에러 반환
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "서버 내부 오류");
            }
        }
    }

    // 인증이 필요 없는 경로를 확인하는 메서드
    private boolean shouldSkipFilter(HttpServletRequest request) {
        // 인증이 필요 없는 경로 목록
        final List<String> excludedPaths = Arrays.asList(
                "/api/auth/login",
                "/api/auth/signup",
                "/api/auth/refresh",
                "/api/public"   // 퍼블릭 API 경로 등
        );

        String path = request.getRequestURI();

        // 경로가 제외 목록에 포함되어 있는지 확인
        return excludedPaths.stream()
                .anyMatch(excludedPath ->
                        path.startsWith(excludedPath) ||
                                path.equals(excludedPath));
    }

    private void authenticateUser(String token) {
        String studentNumber =jwtTokenProvider.getStudentNumberFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(studentNumber);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("JWT 인증 성공 : {}",studentNumber);
    }

    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
}
