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
import org.univcabi.univcabi.cabinet.vo.CabinetPageVo;
import org.univcabi.univcabi.cabinet.vo.CabinetSearchDetailVo;
import org.univcabi.univcabi.cabinet.vo.CabinetSearchVo;
import org.univcabi.univcabi.cabinet.vo.CabinetVo;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.support.HibernateQueryCounter;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        int cabinetCount = 100;
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

    @Test
    void findCabinetsByBuildingAndFloor_throwsWhenOnePositionIsMissing() {
        int cabinetCount = 100;
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

            // 마지막 사물함(A100)은 위치 데이터를 만들지 않는다: Cabinet 100개, Position 99개.
            if (i == cabinetCount - 1) {
                continue;
            }

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
        assertThatThrownBy(() -> cabinetService.findCabinetsByBuildingAndFloor(requestVo))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> assertThat(exception.getStatus())
                                .isEqualTo(ExceptionStatus.CABINET_POSITION_NOT_FOUND));
        System.out.println("=== findCabinetsByBuildingAndFloor SQL 끝 ===");
    }

    /**
     * 캐비닛 N개를 서로 다른 빌딩에 만들고, 원하는 경우 소유자(User/Authn)까지 채운다.
     * cabinetNumber는 모두 "A"로 시작해 키워드 검색에서 재사용할 수 있다.
     */
    private void seedCabinets(int count, boolean withOwner) {
        for (int i = 0; i < count; i++) {
            Building building = Building.builder()
                    .name(BuildingName.공학1관)
                    .floor(i + 1)
                    .section("A")
                    .width(10)
                    .height(10)
                    .build();
            entityManager.persist(building);

            Cabinet.CabinetBuilder cabinetBuilder = Cabinet.builder()
                    .buildingId(building)
                    .cabinetNumber("A" + (i + 1))
                    .status(CabinetStatus.AVAILABLE);

            if (withOwner) {
                User owner = User.builder()
                        .name("캐비닛 소유자 " + i)
                        .affiliation("컴퓨터공학과")
                        .isVisible(true)
                        .build();
                entityManager.persist(owner);

                Authn authn = Authn.builder()
                        .studentNumber(String.valueOf(202800000 + i))
                        .password("test-password")
                        .role(AuthnRole.NORMAL)
                        .user(owner)
                        .build();
                entityManager.persist(authn);

                cabinetBuilder.userId(owner);
            }

            entityManager.persist(cabinetBuilder.build());
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAllCabinetInfo_queryCountDoesNotGrowWithCabinetCount() {
        int cabinetCount = 10;
        seedCabinets(cabinetCount, true);

        HibernateQueryCounter queryCounter = new HibernateQueryCounter(entityManager);
        queryCounter.reset();

        Page<CabinetVo> page = cabinetService.findAllCabinetInfo(new CabinetPageVo(0, cabinetCount));

        long queryCount = queryCounter.count();
        System.out.println("=== findAllCabinetInfo executed SQL count (cabinets=" + cabinetCount + "): " + queryCount + " ===");

        assertThat(page.getContent()).hasSize(cabinetCount);
        // Page 조회를 위한 content(1) + count(1) 쿼리 2건 고정이어야 한다.
        assertThat(queryCount).isEqualTo(2);
    }

    @Test
    void searchCabinetByKeyword_queryCountDoesNotGrowWithCabinetCount() {
        // 서비스에서 페이지 크기를 5로 고정하므로 그에 맞춰 데이터를 구성한다.
        int cabinetCount = 5;
        seedCabinets(cabinetCount, true);

        HibernateQueryCounter queryCounter = new HibernateQueryCounter(entityManager);
        queryCounter.reset();

        List<CabinetVo> result = cabinetService.searchCabinetByKeyword(new CabinetSearchVo("A"));

        long queryCount = queryCounter.count();
        System.out.println("=== searchCabinetByKeyword executed SQL count (cabinets=" + cabinetCount + "): " + queryCount + " ===");

        assertThat(result).hasSize(cabinetCount);
        // content(1) + count(1) 쿼리 2건 고정이어야 한다.
        assertThat(queryCount).isEqualTo(2);
    }

    @Test
    void searchDetailByKeyword_queryCountDoesNotGrowWithCabinetCount() {
        int cabinetCount = 10;
        seedCabinets(cabinetCount, true);

        HibernateQueryCounter queryCounter = new HibernateQueryCounter(entityManager);
        queryCounter.reset();

        Page<CabinetVo> page = cabinetService.searchDetailByKeyword(new CabinetSearchDetailVo("A", 0, cabinetCount));

        long queryCount = queryCounter.count();
        System.out.println("=== searchDetailByKeyword executed SQL count (cabinets=" + cabinetCount + "): " + queryCount + " ===");

        assertThat(page.getContent()).hasSize(cabinetCount);
        // content(1) + count(1) 쿼리 2건 고정이어야 한다.
        assertThat(queryCount).isEqualTo(2);
    }
}
