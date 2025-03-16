package org.univcabi.univcabi.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.univcabi.univcabi.auth.security.JwtAuthenticationFilter;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.CustomUserDetailsService;

@Configuration
@RequiredArgsConstructor
@ComponentScan(basePackages = {"org.univcabi.auth"})
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;


    @Bean
    public UserDetailsService userDetailsService(){
        return customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws  Exception{
        http.csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/authn/login","/authn/create","/authn/delete").permitAll() //로그인 api는 인증 없이 가능
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider,customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class); // JWT 인증 필터 추가

        return http.build();
    }
}

