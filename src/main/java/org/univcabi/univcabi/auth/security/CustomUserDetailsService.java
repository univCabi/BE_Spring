package org.univcabi.univcabi.auth.security;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.exception.ServiceException;

import java.util.List;

import static org.univcabi.univcabi.exception.ExceptionStatus.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AuthnRepository authnRepository;

    @Override
    public UserDetails loadUserByUsername(String studentNumber) throws UsernameNotFoundException {
        Authn authn = authnRepository.findByStudentNumber(studentNumber)
                .orElseThrow(()-> new ServiceException(USER_NOT_FOUND));

        String roleName = "ROLE_" + authn.getRole().name(); // ROLE_ADMIN, ROLE_NORMAL

        return User.builder()
                .username(authn.getStudentNumber())
                .password(authn.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(roleName))) // 이후 해당 권한 정보로 controller에서 권한 기반 접근 제어가 가능 ex) @PreAuthorize("hasRole('ADMIN')")
                .build();
    }

}
