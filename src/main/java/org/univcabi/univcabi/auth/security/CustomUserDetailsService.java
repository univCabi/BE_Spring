package org.univcabi.univcabi.auth.security;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.repository.AuthnRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AuthnRepository authnRepository;

    @Override
    public UserDetails loadUserByUsername(String studentNumber) throws UsernameNotFoundException {
        Authn authn = authnRepository.findByStudentNumber(studentNumber)
                .orElseThrow(()-> new UsernameNotFoundException("유저를 찾을 수 없습니다: "+studentNumber));

        return User.builder()
                .username(authn.getStudentNumber())
                .password(authn.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }

}
