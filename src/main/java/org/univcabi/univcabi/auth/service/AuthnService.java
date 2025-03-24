package org.univcabi.univcabi.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.auth.dto.AuthnRequestDto;
import org.univcabi.univcabi.auth.dto.AuthnResponseDto;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


@Service
@Slf4j
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

    private List<AuthnRequestDto> extractMockUser() {
        try{
            ObjectMapper mapper = new ObjectMapper(); // Json -> Java 변환 도구
            InputStream input = new ClassPathResource("mock/authn.json").getInputStream(); // mock/authn.json 파일 읽기

            return Arrays.asList( // requestDto 객체 배열 생성
                    mapper.readValue(input, AuthnRequestDto[].class));

            }catch (IOException e){
            log.error("mock/authn.json 파일 읽기 실패", e);
            return List.of();
        }
    }

    public void createUser() {
            List<AuthnRequestDto> mockUsers = extractMockUser();

            List<Authn> entities = mockUsers.stream()  // requestDto 객체 배열로 부터 Entity 배열 생성
                    .map(dto -> Authn.builder()
                            .studentNumber(dto.getStudentNumber())
                            .password(dto.getPassword())
                            .role(AuthnRole.ADMIN)
                            .deletedAt(null)
                            .build()).toList();

            authnRepository.saveAll(entities);

    }

    @Transactional
    public void deleteUser(String studentNumber){
        List<AuthnRequestDto> mockUsers = extractMockUser();

        List<String> studentNumbers = mockUsers.stream().map(AuthnRequestDto::getStudentNumber).toList(); // 객체 배열로 부터 studentNumber 추출

        studentNumbers.forEach(authnRepository::deleteByStudentNumber); // studentNumber 로 유저 삭제
    }
}