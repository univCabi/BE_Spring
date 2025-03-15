package org.univcabi.univcabi.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.auth.dto.AuthnRequestDto;
import org.univcabi.univcabi.auth.dto.AuthnResponseDto;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;

@Service
@RequiredArgsConstructor
public class AuthnService {

    private final AuthnRepository authnRepository;

    public AuthnResponseDto login(AuthnRequestDto requestDto){
        Authn authn = authnRepository.findByStudentNumber(requestDto.getStudentNumber())
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if(!authn.getPassword().equals(requestDto.getPassword())){
            throw new SecurityException("비밀번호 불일치");
        }

        return AuthnResponseDto.builder()
                .studentNumber(authn.getStudentNumber())
                .message("로그인 성공")
                .build();
    }

    public void createUser(AuthnRequestDto requestDto) {

        Authn newUser = Authn.builder()
                .studentNumber("202213185")
                .password("202213185")
                .role(AuthnRole.NORMAL)
                .deletedAt(null)
                .build();

        authnRepository.save(newUser);

    }

    @Transactional
    public void deleteUser(String studentNumber){
//        authnRepository.deleteByStudentNumber(studentNumber);
        // Test 용
        authnRepository.deleteByStudentNumber("202213185");
    }
}