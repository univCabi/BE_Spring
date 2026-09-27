package org.univcabi.univcabi.auth.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.auth.vo.AuthnCreateRequestVo;
import org.univcabi.univcabi.auth.vo.AuthnCreateResponseVo;
import org.univcabi.univcabi.auth.vo.AuthnDeleteVo;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({AuthnService.class, QueryDslConfig.class})
class AuthnServiceIntegrationTest {

    @Autowired AuthnService authnService;
    @Autowired AuthnRepository authnRepository;
    @Autowired EntityManager entityManager;

    @Test
    void createUser_persistsUserProfileAndLinkedAuthn() {
        Building building = Building.builder()
                .name(BuildingName.공학1관)
                .floor(2)
                .section("A")
                .width(10)
                .height(5)
                .build();
        entityManager.persist(building);
        entityManager.flush();
        Long buildingId = building.getId();
        entityManager.clear();

        AuthnCreateRequestVo request = new AuthnCreateRequestVo(
                "회원가입 테스트 사용자",
                "컴퓨터공학과",
                "010-1234-5678",
                "202600002",
                "test-password",
                AuthnRole.NORMAL,
                BuildingName.공학1관,
                2,
                "A"
        );

        AuthnCreateResponseVo response = authnService.createUser(request);

        entityManager.flush();
        entityManager.clear();

        Authn authn = authnRepository.findByStudentNumber(request.studentNumber()).orElseThrow();
        assertThat(authn.getId()).isNotNull();
        assertThat(authn.getStudentNumber()).isEqualTo(request.studentNumber());
        assertThat(authn.getPassword()).isEqualTo(request.password());
        assertThat(authn.getRole()).isEqualTo(request.role());
        assertThat(authn.getDeletedAt()).isNull();
        assertThat(authn.getUser()).isNotNull();

        User user = entityManager.find(User.class, authn.getUser().getId());
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo(request.name());
        assertThat(user.getAffiliation()).isEqualTo(request.affiliation());
        assertThat(user.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(user.getIsVisible()).isTrue();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getBuilding()).isNotNull();
        assertThat(user.getBuilding().getId()).isEqualTo(buildingId);
        assertThat(user.getBuilding().getName()).isEqualTo(request.buildingName());
        assertThat(user.getBuilding().getFloor()).isEqualTo(request.floor());
        assertThat(user.getBuilding().getSection()).isEqualTo(request.section());
        assertThat(user.getAuthn().getId()).isEqualTo(authn.getId());
        assertThat(response).isEqualTo(new AuthnCreateResponseVo(request.name(), request.studentNumber()));
    }

    @Test
    void softDeleteUserByStudentNumber_persistsDeletedAt() {
        String studentNumber = "202600001";
        User user = User.builder()
                .name("탈퇴 테스트 사용자")
                .affiliation("컴퓨터공학과")
                .isVisible(true)
                .build();
        entityManager.persist(user);

        entityManager.persist(Authn.builder()
                .studentNumber(studentNumber)
                .password("test-password")
                .role(AuthnRole.NORMAL)
                .user(user)
                .build());
        entityManager.flush();
        entityManager.clear();

        authnService.softDeleteUserByStudentNumber(new AuthnDeleteVo(studentNumber));

        entityManager.flush();
        entityManager.clear();

        Authn reloaded = authnRepository.findByStudentNumber(studentNumber).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isNotNull();
    }
}
