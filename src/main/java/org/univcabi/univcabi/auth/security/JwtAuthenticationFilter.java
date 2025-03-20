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

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException{

        String token = resolveToken(request);

        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    authenticateUser(token);
                }
            } catch (ExpiredJwtException e) {
                log.warn("AccessToken이 만료됨. RefreshToken으로 재발급 시도");

                String refreshToken = tokenService.getRefreshTokenFromCookie(request);
                if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                    String studentNumber = jwtTokenProvider.getStudentNumberFromToken(refreshToken);
                    String storedRefreshToken = tokenService.getRefreshToken(studentNumber);

                    if (refreshToken.equals(storedRefreshToken)) {
                        String newAccessToken = jwtTokenProvider.generateAccessToken(studentNumber, "USER");


                        response.setHeader("Authorization","Bearer "+newAccessToken);
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
            }
        }

        filterChain.doFilter(request, response);
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
