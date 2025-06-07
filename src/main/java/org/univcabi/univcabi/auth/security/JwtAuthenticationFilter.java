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
import org.univcabi.univcabi.auth.service.AuthnService;
import org.univcabi.univcabi.auth.service.TokenService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.univcabi.univcabi.util.TokenUtils.resolveToken;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

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
            String accessToken = resolveToken(request);
            if (jwtTokenProvider.validateToken(accessToken)) {
                authenticateUser(accessToken);
                filterChain.doFilter(request, response); // 누락시에 controller 로 그 다음 요청이 이동하지않음 중요!
                return;
            } else{
                log.warn("AccessToken 만료됨");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"messages\": [{\"token_class\": \"AccessToken\"}]}");
                return;}
        }catch (Exception e) {
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
                "/api/authn/login",
                "/authn/login",
                "/api/auth/signup",
                "/api/auth/refresh",
                "/api/public",   // 퍼블릭 API 경로 등
                "/api/user/mockup",
                "/authn/token/access",
                "/swagger-ui.html",
                "/v3/api-docs",
                "/swagger-ui"
        );

        String path = request.getRequestURI();

        // 경로가 제외 목록에 포함되어 있는지 확인
        return excludedPaths.stream()
                .anyMatch(excludedPath ->
                        path.startsWith(excludedPath) ||
                                path.equals(excludedPath));
    }

    private void authenticateUser(String token) {
        String studentNumber = jwtTokenProvider.getStudentNumberFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(studentNumber);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());

        // JWT는 stateless 이기 때문에 매 요청마다 토큰을 검사해서 인증 정보를 새로 세팅 해줌
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("JWT 인증 성공 : {}",studentNumber);
    }

}
