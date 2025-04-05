package org.univcabi.univcabi.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.auth.vo.AuthnCreateVo;
import org.univcabi.univcabi.auth.vo.AuthnDeleteVo;
import org.univcabi.univcabi.auth.vo.AuthnLoginVo;
import org.univcabi.univcabi.auth.vo.AuthnTokenGenerateVo;
import org.univcabi.univcabi.exception.ServiceException;

import java.time.LocalDateTime;

import static org.univcabi.univcabi.exception.ExceptionStatus.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthnService {

    private final AuthnRepository authnRepository;

    @Transactional
    public AuthnCreateVo createUser(AuthnCreateVo requestVo) {

        // 중복 회원인지 검사
        if(authnRepository.existsByStudentNumber(requestVo.studentNumber())){
            throw new ServiceException(AUTH_DUPLICATE_STUDENT_NUMBER);
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
                .orElseThrow(() -> new ServiceException(USER_NOT_FOUND));


        // 삭제된 유저인지 검사
        if (authn.getDeletedAt() != null){
            throw new ServiceException(AUTH_DELETED_USER);
        }

        authn.setDeletedAtBySoftDelete(LocalDateTime.now());
        authnRepository.save(authn);

        AuthnDeleteVo responseVo = new AuthnDeleteVo(authn.getStudentNumber());

        return responseVo;
    }

    public AuthnTokenGenerateVo loginByStudentNumberAndPassword(AuthnLoginVo requestVo){
        Authn authn = authnRepository.findByStudentNumber(requestVo.studentNumber())
                .orElseThrow(()-> new ServiceException(USER_NOT_FOUND));


        // 올바른 비밀번호인지 검사
        if(!authn.getPassword().equals(requestVo.password())){
            throw new ServiceException(AUTH_MISMATCH_PASSWORD);
        }

        return new AuthnTokenGenerateVo(authn.getStudentNumber(), authn.getRole());
    }

    public AuthnRole getUserRole(String studentNumber){
        return authnRepository.findByStudentNumber(studentNumber)
                .map(Authn::getRole)
                .orElseThrow(()->new ServiceException(USER_NOT_FOUND));
    }

}