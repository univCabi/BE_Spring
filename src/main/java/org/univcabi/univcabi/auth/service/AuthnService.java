package org.univcabi.univcabi.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.auth.vo.*;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.repository.BuildingRepository;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;
import org.univcabi.univcabi.user.vo.AdminUserCreateVo;

import java.time.LocalDateTime;

import static org.univcabi.univcabi.exception.ExceptionStatus.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthnService {

    private final AuthnRepository authnRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;

    @Transactional
    public AuthnCreateResponseVo createUser(AuthnCreateRequestVo requestVo) {

        // 중복 회원인지 검사
        if(authnRepository.existsByStudentNumber(requestVo.studentNumber())){
            throw new ServiceException(AUTH_DUPLICATE_STUDENT_NUMBER);
        }

        Building building = findBuildingOrNull(requestVo);

        User user = User.builder()
                .name(requestVo.name())
                .affiliation(requestVo.affiliation())
                .phoneNumber(requestVo.phoneNumber())
                .building(building)
                .isVisible(true)
                .build();

        userRepository.save(user);

        Authn authn = Authn.builder()
                .studentNumber(requestVo.studentNumber())
                .password(requestVo.password())
                .role(requestVo.role())
                .user(user)
                .deletedAt(null)
                .build();

        // 회원 저장
        authnRepository.save(authn);

        AuthnCreateResponseVo responseVo = new AuthnCreateResponseVo(
                user.getName(),
                authn.getStudentNumber()
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

    private Building findBuildingOrNull(AuthnCreateRequestVo vo) {
        boolean allNull = vo.buildingName() == null && vo.floor() == null && vo.section() == null;
        boolean allPresent = vo.buildingName() != null && vo.floor() != null && vo.section() != null;

        if (allNull) {
            return null;                                   // 빌딩 미지정
        }
        if (!allPresent) {
            throw new ServiceException(INVALID_BUILDING_INFO);   // 일부만 입력
        }
        return buildingRepository.findBuildingByNameAndFloorAndSection(
                        vo.buildingName(), vo.floor(), vo.section())
                .orElseThrow(() -> new ServiceException(BUILDING_NOT_FOUND));
    }

}