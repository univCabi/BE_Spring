package org.univcabi.univcabi.cabinet.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Commit;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.configs.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 수동 EXPLAIN 실습을 위해 종료 후에도 테이블과 데이터를 남긴다.
// 다시 실행하면 create에 의해 기존 테스트 테이블과 직접 만든 인덱스가 초기화된다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@Import(QueryDslConfig.class)
class CabinetIndexIntegrationTest {
    @Autowired CabinetRepository cabinetRepository;
    @Autowired EntityManager entityManager;

    @Test
    void findCabinetByBuildingAndFloor_printSqlForManualExplain() {
        Building building = Building.builder()
                .name(BuildingName.공학1관)
                .floor(1)
                .section("A")
                .width(1)
                .height(1)
                .build();
        entityManager.persist(building);

        Cabinet cabinet = cabinetRepository.save(Cabinet.builder()
                .buildingId(building)
                .cabinetNumber("101")
                .status(CabinetStatus.AVAILABLE)
                .build());
        Long cabinetId = cabinet.getId();

        entityManager.flush();
        entityManager.clear();

        // 이 호출에서 출력되는 SELECT와 바인딩 값을 복사해 MySQL에서 직접 EXPLAIN한다.
        List<Cabinet> result = cabinetRepository
                .findCabinetByBuildingAndFloor(BuildingName.공학1관, 1);

        assertThat(result).extracting(Cabinet::getId).containsExactly(cabinetId);
    }
}
