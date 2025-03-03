package org.univcabi.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.univcabi.auth.dto.AuthnRequestDto;
import org.univcabi.auth.dto.AuthnResponseDto;
import org.univcabi.auth.entity.Authn;
import org.univcabi.auth.entity.AuthnRole;
import org.univcabi.auth.repository.AuthnRepository;

@Service
@RequiredArgsConstructor
public class AuthnService {

    private  final AuthnRepository authnRepository;

    public AuthnResponseDto login(AuthnRequestDto requestDto){
        Authn authn = authnRepository.findByStudentNumber(requestDto.getStudentNumber())
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if(!authn.getPassword().equals(requestDto.getPassword())){
            throw new SecurityException("비밀번호 불일치");
        }

        return  new AuthnResponseDto("로그인 성공");
    }

    public void createUser(AuthnRequestDto requestDto) {
//        Authn newUser = Authn.builder()
//                .studentNumber(requestDto.getStudentNumber())
//                .password(requestDto.getPassword())
//                .role(AuthnRole.NORMAL)
//                .build();
        // Test 용
        Authn newUser = Authn.builder()
                .studentNumber("202213185")
                .password("202213185")
                .role(AuthnRole.NORMAL)
                .build();

        authnRepository.save(newUser);

    }

    public void deleteUser(String studentNumber){
//        authnRepository.deleteByStudentNumber(studentNumber);
        // Test 용
        authnRepository.deleteByStudentNumber("202213185");
    }
}