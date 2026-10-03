package org.univcabi.univcabi.user.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.user.entity.User;

import java.time.LocalDateTime;
import org.univcabi.univcabi.support.HibernateQueryCounter;
import org.univcabi.univcabi.user.vo.UserProfileVo;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({QueryDslConfig.class, UserService.class})
class
UserServiceSqlIntegrationTest {

    @Autowired UserService userService;
    @Autowired EntityManager entityManager;

    @Test
    void getUserProfileByStudentNumber_fetchesProfileAndRentalWithTwoQueries() {
        int userCount = 10;
        LocalDateTime expiredAt = LocalDateTime.of(2027, 1, 1, 0, 0);

        // 사용자 건물과 사물함 건물을 구분해 동일 엔티티 재사용으로 조회가 가려지지 않게 한다.
        Building userBuilding = Building.builder()
                .name(BuildingName.공학1관)
                .floor(2)
                .section("B")
                .width(1)
                .height(1)
                .build();
        entityManager.persist(userBuilding);

        Building cabinetBuilding = Building.builder()
                .name(BuildingName.공학1관)
                .floor(1)
                .section("A")
                .width(1)
                .height(1)
                .build();
        entityManager.persist(cabinetBuilding);

        for (int i = 0; i < userCount; i++) {
            String studentNumber = String.valueOf(202600001 + i);
            User user = User.builder()
                    .name("프로필 SQL 조회 사용자 " + i)
                    .affiliation("컴퓨터공학과")
                    .phoneNumber("01012345678")
                    .building(userBuilding)
                    .isVisible(true)
                    .build();
            entityManager.persist(user);

            entityManager.persist(Authn.builder()
                    .studentNumber(studentNumber)
                    .password("test-password")
                    .role(AuthnRole.NORMAL)
                    .user(user)
                    .build());

            Cabinet cabinet = Cabinet.builder()
                    .buildingId(cabinetBuilding)
                    .userId(user)
                    // 프로필 서비스가 Integer.parseInt()로 변환하므로 숫자 문자열을 사용한다.
                    .cabinetNumber(String.valueOf(101 + i))
                    .status(CabinetStatus.USING)
                    .build();
            entityManager.persist(cabinet);
            entityManager.persist(CabinetHistory.createRentHistory(
                    user, cabinet, expiredAt));

        }

        entityManager.flush();
        entityManager.clear();

        HibernateQueryCounter queryCounter = new HibernateQueryCounter(entityManager);

        // 단건 서비스를 반복 호출한다. 전체 SQL 증가만으로 목록 N+1이라고 판단하지 않는다.
        for (int i = 0; i < userCount; i++) {
            entityManager.clear(); // 앞선 사용자 조회에서 로딩한 건물 등의 1차 캐시 제거
            String studentNumber = String.valueOf(202600001 + i);
            queryCounter.reset();
            UserProfileVo profile = userService.getUserProfileByStudentNumber(studentNumber);

            assertThat(profile.name()).isEqualTo("프로필 SQL 조회 사용자 " + i);
            assertThat(profile.studentNumber()).isEqualTo(studentNumber);
            assertThat(profile.affiliation()).isEqualTo("컴퓨터공학과");
            assertThat(profile.phoneNumber()).isEqualTo("01012345678");
            assertThat(profile.isVisible()).isTrue();
            assertThat(profile.rentCabinetInfoVo()).isNotNull();
            assertThat(profile.rentCabinetInfoVo().building()).isEqualTo(BuildingName.공학1관.name());
            assertThat(profile.rentCabinetInfoVo().floor()).isEqualTo(1);
            assertThat(profile.rentCabinetInfoVo().cabinetNumber()).isEqualTo(101 + i);
            assertThat(profile.rentCabinetInfoVo().status()).isEqualTo(CabinetStatus.USING.name());
            assertThat(profile.rentCabinetInfoVo().startDate()).isNotNull();
            assertThat(profile.rentCabinetInfoVo().endDate()).isEqualTo(expiredAt);
            // 사용자 조회(1) + 최신 대여 이력 조회(1). 연관 정보 접근도 측정에 포함한다.
            assertThat(queryCounter.count()).isEqualTo(2);
        }
    }
}
