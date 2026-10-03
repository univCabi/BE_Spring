package org.univcabi.univcabi.user.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.auth.service.AuthnService;
import org.univcabi.univcabi.auth.vo.AuthnCreateRequestVo;
import org.univcabi.univcabi.auth.vo.AuthnCreateResponseVo;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.vo.AdminUserCreateVo;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({QueryDslConfig.class, AuthnService.class, UserService.class})
class UserCreationBuildingIntegrationTest {
    @Autowired EntityManager entityManager;
    @Autowired AuthnService authnService;
    @Autowired UserService userService;
    @Autowired AuthnRepository authnRepository;

    // 일반 회원의 건물 지정 성공은 AuthnServiceIntegrationTest에서 이미 검증한다.
    @ParameterizedTest(name = "관리자={0}, 건물 지정={1}")
    @CsvSource({"false,false", "true,false", "true,true"})
    void createUser_persistsProfileAndOptionalBuilding(boolean admin, boolean withBuilding) {
        Building building = null;
        if (withBuilding) {
            building = Building.builder().name(BuildingName.공학1관).floor(2).section("A")
                    .width(10).height(5).build();
            entityManager.persist(building);
            entityManager.flush();
            entityManager.clear();
        }

        createUser(admin, withBuilding ? BuildingName.공학1관 : null,
                withBuilding ? 2 : null, withBuilding ? "A" : null);
        entityManager.flush();
        entityManager.clear();

        Authn authn = authnRepository.findByStudentNumber("202600003").orElseThrow();
        User user = authn.getUser();
        assertThat(authn.getRole()).isEqualTo(admin ? AuthnRole.ADMIN : AuthnRole.NORMAL);
        assertThat(authn.getPassword()).isEqualTo("test-password");
        assertThat(user.getId()).isNotNull();
        assertThat(user.getName()).isEqualTo("건물 분기 테스트");
        assertThat(user.getAffiliation()).isEqualTo("컴퓨터공학과");
        assertThat(user.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(user.getIsVisible()).isTrue();
        assertThat(user.getAuthn().getId()).isEqualTo(authn.getId());
        if (withBuilding) {
            assertThat(user.getBuilding().getId()).isEqualTo(building.getId());
        } else {
            assertThat(user.getBuilding()).isNull();
        }
    }

    static Stream<Arguments> partialBuildings() {
        // 세 필드의 일부만 존재하는 여섯 조합을 두 생성 경로에 각각 적용한다.
        return Stream.of(false, true).flatMap(admin -> IntStream.range(1, 7)
                .mapToObj(mask -> Arguments.of(admin,
                        (mask & 1) != 0 ? BuildingName.공학1관 : null,
                        (mask & 2) != 0 ? 2 : null,
                        (mask & 4) != 0 ? "A" : null)));
    }

    @ParameterizedTest
    @MethodSource("partialBuildings")
    void createUser_rejectsPartialBuilding(boolean admin, BuildingName name, Integer floor, String section) {
        assertThatThrownBy(() -> createUser(admin, name, floor, section))
                .isInstanceOfSatisfying(ServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(ExceptionStatus.INVALID_BUILDING_INFO));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createUser_rejectsNonexistentBuilding(boolean admin) {
        assertThatThrownBy(() -> createUser(admin, BuildingName.공학1관, 2, "A"))
                .isInstanceOfSatisfying(ServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(ExceptionStatus.BUILDING_NOT_FOUND));
    }

    private void createUser(boolean admin, BuildingName name, Integer floor, String section) {
        if (admin) {
            userService.createAdminUser(new AdminUserCreateVo(
                    "건물 분기 테스트", "컴퓨터공학과", "01012345678", "202600003",
                    "test-password", AuthnRole.ADMIN, name, floor, section));
        } else {
            assertThat(authnService.createUser(new AuthnCreateRequestVo(
                    "건물 분기 테스트", "컴퓨터공학과", "01012345678", "202600003",
                    "test-password", AuthnRole.NORMAL, name, floor, section)))
                    .isEqualTo(new AuthnCreateResponseVo("건물 분기 테스트", "202600003"));
        }
    }
}
