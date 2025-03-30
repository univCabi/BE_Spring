package org.univcabi.univcabi.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.auth.dto.request.AuthnCreateRequestDto;
import org.univcabi.univcabi.auth.dto.response.AuthnCreateResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnDeleteResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnResponseDto;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.auth.vo.AuthnCreateVo;
import org.univcabi.univcabi.auth.vo.AuthnDeleteVo;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthnService {

    private final AuthnRepository authnRepository;

    public AuthnCreateResponseDto createUser(AuthnCreateVo requestVo) {

        // 중복 회원인지 검사
        if(authnRepository.existsByStudentNumber(requestVo.studentNumber())){
            throw new IllegalArgumentException("이미 존재하는 회원입니다.");
        }

        Authn user = Authn.builder()
                .studentNumber(requestVo.studentNumber())
                .password(requestVo.password())
                .role(requestVo.role())
                .deletedAt(null)
                .build();

        // 회원 저장
        authnRepository.save(user);

        return AuthnCreateResponseDto.builder()
                .studentNumber(user.getStudentNumber())
                .message("회원 생성 성공")
                .build();
    }

    @Transactional
    public AuthnDeleteResponseDto deleteUser(AuthnDeleteVo requestVo){
        Authn authn = authnRepository.findByStudentNumber(requestVo.studentNumber())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다"));

        if (authn.getDeletedAt() != null){
            throw new IllegalArgumentException("이미 탈퇴된 유저입니다.");
        }

        authn.setDeletedAtBySoftDelete(LocalDateTime.now());
        authnRepository.save(authn);

        return AuthnDeleteResponseDto.builder()
                .studentNumber(authn.getStudentNumber())
                .message("관리자에 의해 해당 유저는 삭제되었습니다.")
                .build();
    }

    public AuthnResponseDto login(AuthnCreateRequestDto requestDto){
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

    private List<AuthnCreateRequestDto> extractMockUser() {
        try{
            ObjectMapper mapper = new ObjectMapper(); // Json -> Java 변환 도구
            InputStream input = new ClassPathResource("mock/authn.json").getInputStream(); // mock/authn.json 파일 읽기

            return Arrays.asList( // requestDto 객체 배열 생성
                    mapper.readValue(input, AuthnCreateRequestDto[].class));

            }catch (IOException e){
            log.error("mock/authn.json 파일 읽기 실패", e);
            return List.of();
        }
    }

}