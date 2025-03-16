package org.univcabi.univcabi.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Token;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.client.RestTemplate;
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

       if(token != null){
           if(jwtTokenProvider.validateToken(token)){
               authenticateUser(token);
           }
           else if(jwtTokenProvider.isTokenExpired(token)){
               log.info("AccessToken 만료");

               String refreshToken = tokenService.getRefreshTokenFromCookie(request);
               if(refreshToken!=null && jwtTokenProvider.validateToken(refreshToken)){
                   RestTemplate restTemplate = new RestTemplate();
                   String url = "http://localhost:8080/authn/token/access";
                   ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                           url,null,String.class);
                   if(responseEntity.getStatusCode().is2xxSuccessful()){
                       log.info("새로운 Access Token 발급");

                       authenticateUser(responseEntity.getBody());
                   }
               }

           }
       }

        filterChain.doFilter(request,response);
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
