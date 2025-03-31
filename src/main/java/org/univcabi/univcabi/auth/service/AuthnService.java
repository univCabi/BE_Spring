package org.univcabi.univcabi.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.auth.dto.response.AuthnCreateResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnDeleteResponseDto;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.auth.vo.AuthnCreateVo;
import org.univcabi.univcabi.auth.vo.AuthnDeleteVo;
import org.univcabi.univcabi.auth.vo.AuthnLoginVo;
import org.univcabi.univcabi.auth.vo.AuthnTokenGenerateVo;

import java.time.LocalDateTime;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthnService {

    private final AuthnRepository authnRepository;

    public AuthnCreateVo createUser(AuthnCreateVo requestVo) {

        // 중복 회원인지 검사
        // 예외 처리 필요 나중에! -> 지금 예외가 500 뜨는 상태
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

        AuthnCreateVo responseVo = new AuthnCreateVo(
                user.getStudentNumber(),
                user.getPassword(),
                user.getRole()
        );

        return responseVo;
    }

    @Transactional
    public AuthnDeleteVo softDeleteUserByStudentNumber(AuthnDeleteVo requestVo){
        Authn authn = authnRepository.findByStudentNumber(requestVo.studentNumber())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다"));

        if (authn.getDeletedAt() != null){
            throw new IllegalArgumentException("이미 탈퇴된 유저입니다.");
        }

        authn.setDeletedAtBySoftDelete(LocalDateTime.now());
        authnRepository.save(authn);

        AuthnDeleteVo responseVo = new AuthnDeleteVo(authn.getStudentNumber());

        return responseVo;
    }

    public AuthnTokenGenerateVo loginByStudentNumberAndPassword(AuthnLoginVo requestVo){
        Authn authn = authnRepository.findByStudentNumber(requestVo.studentNumber())
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if(!authn.getPassword().equals(requestVo.password())){
            throw new SecurityException("비밀번호 불일치");
        }

        return new AuthnTokenGenerateVo(authn.getStudentNumber(), authn.getRole());
    }

    public AuthnRole getUserRole(String studentNumber){
        return authnRepository.findByStudentNumber(studentNumber)
                .map(Authn::getRole)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 유저입니다."));
    }

}