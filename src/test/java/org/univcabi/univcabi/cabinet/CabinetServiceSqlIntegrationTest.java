package org.univcabi.univcabi.cabinet;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetPosition;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.service.CabinetFallbackService;
import org.univcabi.univcabi.cabinet.service.CabinetKafkaProducerService;
import org.univcabi.univcabi.cabinet.service.CabinetRedisService;
import org.univcabi.univcabi.cabinet.service.CabinetUtilService;
import org.univcabi.univcabi.cabinet.service.ReservationQueueManager;
import org.univcabi.univcabi.cabinet.vo.CabinetLocationVo;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.user.entity.User;

import java.util.concurrent.Executor;

@DataJpaTest
@ActiveProfiles("test")
@Import({QueryDslConfig.class, CabinetService.class})
class CabinetServiceSqlIntegrationTest {

    @Autowired CabinetService cabinetService;
    @Autowired EntityManager entityManager;

    // 이 조회에서 사용하지 않는 의존성만 대체한다. 서비스와 JPA Repository는 실제 객체다.
    @MockitoBean RedisTemplate<String, Object> redisTemplate;
    @MockitoBean CabinetKafkaProducerService kafkaProducerService;
    @MockitoBean ReservationQueueManager queueManager;
    @MockitoBean CabinetUtilService cabinetUtilService;
    @MockitoBean CabinetRedisService cabinetRedisService;
    @MockitoBean Executor cabinetTaskExecutor;
    @MockitoBean CabinetFallbackService cabinetFallbackService;

    @Test
    void findCabinetsByBuildingAndFloor_printSql() {
        // N을 변경해 조회 구간에서 발생하는 SELECT를 비교할 수 있다.
        int cabinetCount = 10;
        Building building = Building.builder()
                .name(BuildingName.공학1관)
                .floor(1)
                .section("A")
                .width(cabinetCount)
                .height(1)
                .build();
        entityManager.persist(building);

        for (int i = 0; i < cabinetCount; i++) {
            User user = User.builder()
                    .name("SQL 조회 사용자 " + i)
                    .affiliation("컴퓨터공학과")
                    .isVisible(true)
                    .build();
            entityManager.persist(user);

            Authn authn = Authn.builder()
                    .studentNumber(String.valueOf(202600000 + i))
                    .password("test-password")
                    .role(AuthnRole.NORMAL)
                    .user(user)
                    .build();
            entityManager.persist(authn);

            Cabinet cabinet = Cabinet.builder()
                    .buildingId(building)
                    .userId(user)
                    .cabinetNumber("A" + (i + 1))
                    .status(CabinetStatus.USING)
                    .build();
            entityManager.persist(cabinet);

            // 생성용 API가 없는 엔티티는 테스트에서만 리플렉션으로 구성한다.
            CabinetPosition position = BeanUtils.instantiateClass(CabinetPosition.class);
            ReflectionTestUtils.setField(position, "cabinetId", cabinet);
            ReflectionTestUtils.setField(position, "cabinetXPos", i);
            ReflectionTestUtils.setField(position, "cabinetYPos", 0);
            entityManager.persist(position);
        }

        entityManager.flush();
        entityManager.clear();

        CabinetLocationVo requestVo = new CabinetLocationVo(BuildingName.공학1관, 1, "202600000");
        System.out.println("=== findCabinetsByBuildingAndFloor SQL 시작 (N=" + cabinetCount + ") ===");
        cabinetService.findCabinetsByBuildingAndFloor(requestVo);
        System.out.println("=== findCabinetsByBuildingAndFloor SQL 끝 ===");
    }
}
