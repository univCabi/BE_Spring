package org.univcabi.univcabi.cabinet.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.user.entity.User;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class CabinetCustomRepositoryIntegrationTest {

    @Autowired CabinetRepository cabinetRepository;
    @Autowired EntityManager entityManager;

    @Test
    void findOneCabinetInfoByCabinetId_printSql() {
        User user = User.builder()
                .name("조회 테스트 사용자")
                .affiliation("컴퓨터공학과")
                .isVisible(true)
                .build();
        entityManager.persist(user);

        Building building = Building.builder()
                .name(BuildingName.공학1관)
                .floor(1)
                .section("A")
                .width(1)
                .height(1)
                .build();
        entityManager.persist(building);

        Cabinet cabinet = Cabinet.builder()
                .buildingId(building)
                .userId(user)
                .cabinetNumber("101")
                .status(CabinetStatus.AVAILABLE)
                .build();
        entityManager.persist(cabinet);
        Long cabinetId = cabinet.getId();

        // 준비용 INSERT를 반영하고 1차 캐시를 비운 뒤 실제 조회 SQL을 확인한다.
        entityManager.flush();
        entityManager.clear();

        System.out.println("=== findOneCabinetInfoByCabinetId SQL 시작 ===");
        cabinetRepository.findOneCabinetInfoByCabinetId(cabinetId);
        System.out.println("=== findOneCabinetInfoByCabinetId SQL 끝 ===");
    }
}
