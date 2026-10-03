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
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;
import org.univcabi.univcabi.cabinet.entity.CabinetPosition;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.service.CabinetFallbackService;
import org.univcabi.univcabi.cabinet.service.CabinetKafkaProducerService;
import org.univcabi.univcabi.cabinet.service.CabinetRedisService;
import org.univcabi.univcabi.cabinet.service.CabinetUtilService;
import org.univcabi.univcabi.cabinet.service.ReservationQueueManager;
import org.univcabi.univcabi.cabinet.vo.CabinetByStatusVo;
import org.univcabi.univcabi.cabinet.vo.CabinetDataVo;
import org.univcabi.univcabi.cabinet.vo.CabinetLocationVo;
import org.univcabi.univcabi.cabinet.vo.CabinetPageVo;
import org.univcabi.univcabi.cabinet.vo.CabinetSearchDetailVo;
import org.univcabi.univcabi.cabinet.vo.CabinetSearchVo;
import org.univcabi.univcabi.cabinet.vo.CabinetStatusVo;
import org.univcabi.univcabi.cabinet.vo.CabinetVo;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.support.HibernateQueryCounter;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.ServiceException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    private User persistOwner(String name, String studentNumber) {
        User owner = User.builder()
                .name(name)
                .affiliation("컴퓨터공학과")
                .isVisible(true)
                .build();
        entityManager.persist(owner);

        entityManager.persist(Authn.builder()
                .studentNumber(studentNumber)
                .password("test-password")
                .role(AuthnRole.NORMAL)
                .user(owner)
                .build());
        return owner;
    }

    private Building persistBuilding(int floor) {
        Building building = Building.builder()
                .name(BuildingName.공학1관)
                .floor(floor)
                .section("A")
                .width(10)
                .height(10)
                .build();
        entityManager.persist(building);
        return building;
    }

    private Cabinet persistCabinet(Building building, User owner, String cabinetNumber, CabinetStatus status) {
        Cabinet cabinet = Cabinet.builder()
                .buildingId(building)
                .userId(owner)
                .cabinetNumber(cabinetNumber)
                .status(status)
                .build();
        entityManager.persist(cabinet);
        return cabinet;
    }

    private void persistPosition(Cabinet cabinet, int x, int y) {
        // 생성용 API가 없는 엔티티는 테스트에서만 리플렉션으로 구성한다.
        CabinetPosition position = BeanUtils.instantiateClass(CabinetPosition.class);
        ReflectionTestUtils.setField(position, "cabinetId", cabinet);
        ReflectionTestUtils.setField(position, "cabinetXPos", x);
        ReflectionTestUtils.setField(position, "cabinetYPos", y);
        entityManager.persist(position);
    }

    private void persistHistory(User user, Cabinet cabinet, LocalDateTime expiredAt) {
        entityManager.persist(CabinetHistory.createRentHistory(user, cabinet, expiredAt));
    }

    @Test
    void findCabinetsByBuildingAndFloor_queryCountDoesNotGrowWithCabinetCount() {
        int cabinetCount = 10;
        Building building = persistBuilding(1);

        for (int i = 0; i < cabinetCount; i++) {
            User owner = persistOwner("층별 조회 사용자 " + i, String.valueOf(202900000 + i));
            Cabinet cabinet = persistCabinet(building, owner, "A" + (i + 1), CabinetStatus.USING);
            persistPosition(cabinet, i, 0);
        }

        entityManager.flush();
        entityManager.clear();

        HibernateQueryCounter queryCounter = new HibernateQueryCounter(entityManager);
        queryCounter.reset();

        List<CabinetDataVo> result = cabinetService.findCabinetsByBuildingAndFloor(
                new CabinetLocationVo(BuildingName.공학1관, 1, "202900000"));

        long queryCount = queryCounter.count();
        System.out.println("=== findCabinetsByBuildingAndFloor executed SQL count (cabinets=" + cabinetCount + "): " + queryCount + " ===");

        assertThat(result).hasSize(cabinetCount);
        // Cabinet + Building + User + Authn fetch join 조회(1) + Position IN 조회(1) = 2건 고정이어야 한다.
        assertThat(queryCount).isEqualTo(2);
    }

    @Test
    void findCabinetsByBuildingAndFloor_mapsPositionAndOwnerToEachCabinet() {
        Building building = persistBuilding(1);

        User requester = persistOwner("요청자", "202900100");
        User other = persistOwner("다른 사용자", "202900101");

        Cabinet mine = persistCabinet(building, requester, "A1", CabinetStatus.USING);
        Cabinet othersCabinet = persistCabinet(building, other, "A2", CabinetStatus.USING);
        Cabinet empty = persistCabinet(building, null, "A3", CabinetStatus.AVAILABLE);
        persistPosition(mine, 10, 20);
        persistPosition(othersCabinet, 30, 40);
        persistPosition(empty, 50, 60);

        entityManager.flush();
        entityManager.clear();

        Map<String, CabinetDataVo> result = cabinetService.findCabinetsByBuildingAndFloor(
                        new CabinetLocationVo(BuildingName.공학1관, 1, "202900100"))
                .stream()
                .collect(Collectors.toMap(CabinetDataVo::cabinetNumber, Function.identity()));

        assertThat(result).containsOnlyKeys("A1", "A2", "A3");

        CabinetDataVo mineVo = result.get("A1");
        assertThat(mineVo.cabinetXPos()).isEqualTo(10);
        assertThat(mineVo.cabinetYPos()).isEqualTo(20);
        assertThat(mineVo.username()).isEqualTo("요청자");
        assertThat(mineVo.isMine()).isTrue();
        assertThat(mineVo.isRentAvailable()).isFalse();

        CabinetDataVo othersVo = result.get("A2");
        assertThat(othersVo.cabinetXPos()).isEqualTo(30);
        assertThat(othersVo.cabinetYPos()).isEqualTo(40);
        assertThat(othersVo.username()).isEqualTo("다른 사용자");
        assertThat(othersVo.isMine()).isFalse();

        CabinetDataVo emptyVo = result.get("A3");
        assertThat(emptyVo.cabinetXPos()).isEqualTo(50);
        assertThat(emptyVo.cabinetYPos()).isEqualTo(60);
        assertThat(emptyVo.username()).isNull();
        assertThat(emptyVo.isVisible()).isFalse();
        assertThat(emptyVo.isMine()).isFalse();
        assertThat(emptyVo.isRentAvailable()).isTrue();
    }

    @Test
    void findCabinetsByStatus_queryCountDoesNotGrowWithCabinetCount() {
        int cabinetCount = 10;
        Building building = persistBuilding(1);
        LocalDateTime expiredAt = LocalDateTime.of(2026, 12, 31, 0, 0);

        for (int i = 0; i < cabinetCount; i++) {
            User owner = persistOwner("상태 조회 사용자 " + i, String.valueOf(203000000 + i));
            Cabinet cabinet = persistCabinet(building, owner, "A" + (i + 1), CabinetStatus.USING);
            persistPosition(cabinet, i, 0);
            // 캐비닛마다 히스토리를 2건씩 두어 "최신 1건" 선택이 일괄 조회로 처리되는지 확인한다.
            persistHistory(owner, cabinet, expiredAt.minusDays(10));
            persistHistory(owner, cabinet, expiredAt);
        }

        entityManager.flush();
        entityManager.clear();

        HibernateQueryCounter queryCounter = new HibernateQueryCounter(entityManager);
        queryCounter.reset();

        Page<CabinetByStatusVo> page = cabinetService.findCabinetsByStatus(
                new CabinetStatusVo(CabinetStatus.USING), PageRequest.of(0, cabinetCount));

        long queryCount = queryCounter.count();
        System.out.println("=== findCabinetsByStatus executed SQL count (cabinets=" + cabinetCount + "): " + queryCount + " ===");

        assertThat(page.getContent()).hasSize(cabinetCount);
        // content(1) + count(1) + Position IN(1) + 최신 History IN(1) = 4건 고정이어야 한다.
        assertThat(queryCount).isEqualTo(4);
    }

    @Test
    void findCabinetsByStatus_usesLatestHistoryPerCabinet() {
        Building building = persistBuilding(1);

        User firstOwner = persistOwner("첫 번째 사용자", "203000100");
        User secondOwner = persistOwner("두 번째 사용자", "203000101");
        Cabinet first = persistCabinet(building, firstOwner, "A1", CabinetStatus.USING);
        Cabinet second = persistCabinet(building, secondOwner, "A2", CabinetStatus.USING);
        persistPosition(first, 0, 0);
        persistPosition(second, 1, 0);

        // createdAt은 @PrePersist로 채워져 같은 값일 수 있으므로, 나중에 저장한 히스토리(id가 큰 쪽)가 선택되어야 한다.
        persistHistory(firstOwner, first, LocalDateTime.of(2026, 10, 1, 0, 0));
        persistHistory(firstOwner, first, LocalDateTime.of(2026, 10, 15, 0, 0));
        persistHistory(secondOwner, second, LocalDateTime.of(2026, 11, 1, 0, 0));
        persistHistory(secondOwner, second, LocalDateTime.of(2026, 11, 20, 0, 0));

        entityManager.flush();
        entityManager.clear();

        Map<String, CabinetByStatusVo> result = cabinetService.findCabinetsByStatus(
                        new CabinetStatusVo(CabinetStatus.USING), PageRequest.of(0, 10))
                .getContent()
                .stream()
                .collect(Collectors.toMap(CabinetByStatusVo::cabinetNumber, Function.identity()));

        assertThat(result).containsOnlyKeys("A1", "A2");

        CabinetByStatusVo firstVo = result.get("A1");
        assertThat(firstVo.overDate()).isEqualTo(LocalDateTime.of(2026, 10, 15, 0, 0).toLocalDate());
        assertThat(firstVo.rentalStartDate()).isNotNull();
        assertThat(firstVo.reason()).isEqualTo(CabinetStatus.USING.name());
        assertThat(firstVo.brokenDate()).isNull();
        assertThat(firstVo.position().getCabinetXPos()).isEqualTo(0);

        CabinetByStatusVo secondVo = result.get("A2");
        assertThat(secondVo.overDate()).isEqualTo(LocalDateTime.of(2026, 11, 20, 0, 0).toLocalDate());
        assertThat(secondVo.position().getCabinetXPos()).isEqualTo(1);
    }

    @Test
    void findCabinetsByStatus_returnsEmptyPageWithTotalWhenPageIsOutOfRange() {
        int cabinetCount = 3;
        Building building = persistBuilding(1);
        for (int i = 0; i < cabinetCount; i++) {
            Cabinet cabinet = persistCabinet(building, null, "A" + (i + 1), CabinetStatus.BROKEN);
            persistPosition(cabinet, i, 0);
        }

        entityManager.flush();
        entityManager.clear();

        HibernateQueryCounter queryCounter = new HibernateQueryCounter(entityManager);
        queryCounter.reset();

        Page<CabinetByStatusVo> page = cabinetService.findCabinetsByStatus(
                new CabinetStatusVo(CabinetStatus.BROKEN), PageRequest.of(5, 10));

        long queryCount = queryCounter.count();

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(cabinetCount);
        // 빈 페이지면 Position/History 조회를 건너뛰어 content(1) + count(1) = 2건이어야 한다.
        assertThat(queryCount).isEqualTo(2);
    }

    @Test
    void findCabinetsByStatus_throwsWhenPositionIsMissing() {
        Building building = persistBuilding(1);
        Cabinet withPosition = persistCabinet(building, null, "A1", CabinetStatus.BROKEN);
        persistPosition(withPosition, 0, 0);
        // A2는 위치 데이터를 만들지 않는다.
        persistCabinet(building, null, "A2", CabinetStatus.BROKEN);

        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> cabinetService.findCabinetsByStatus(
                new CabinetStatusVo(CabinetStatus.BROKEN), PageRequest.of(0, 10)))
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> assertThat(exception.getStatus())
                                .isEqualTo(ExceptionStatus.CABINET_POSITION_NOT_FOUND));
    }

    @Test
    void findCabinetsByStatus_usesLargerHistoryIdWhenCreatedAtIsEqual() {
        Building building = persistBuilding(1);
        User owner = persistOwner("동률 이력 사용자", "203000200");
        Cabinet cabinet = persistCabinet(building, owner, "A1", CabinetStatus.USING);
        persistPosition(cabinet, 0, 0);
        CabinetHistory older = CabinetHistory.createRentHistory(
                owner, cabinet, LocalDateTime.of(2026, 10, 15, 0, 0));
        CabinetHistory newer = CabinetHistory.createRentHistory(
                owner, cabinet, LocalDateTime.of(2026, 11, 15, 0, 0));
        entityManager.persist(older);
        entityManager.persist(newer);
        entityManager.flush();
        assertThat(newer.getId()).isGreaterThan(older.getId());

        // createdAt은 @PrePersist 및 updatable=false이므로 DB 값을 직접 고정한다.
        LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        int updated = entityManager.createNativeQuery(
                        "UPDATE cabinet_histories SET created_at = :createdAt WHERE cabinet_id = :cabinetId")
                .setParameter("createdAt", sameCreatedAt)
                .setParameter("cabinetId", cabinet.getId())
                .executeUpdate();
        assertThat(updated).isEqualTo(2);
        entityManager.clear();
        assertThat(entityManager.find(CabinetHistory.class, older.getId()).getCreatedAt()).isEqualTo(sameCreatedAt);
        assertThat(entityManager.find(CabinetHistory.class, newer.getId()).getCreatedAt()).isEqualTo(sameCreatedAt);
        entityManager.clear();

        Page<CabinetByStatusVo> result = cabinetService.findCabinetsByStatus(
                new CabinetStatusVo(CabinetStatus.USING), PageRequest.of(0, 10));

        assertThat(result.getContent()).singleElement().satisfies(vo -> {
            assertThat(vo.cabinetNumber()).isEqualTo("A1");
            assertThat(vo.overDate()).isEqualTo(newer.getExpiredAt().toLocalDate());
        });
    }

}
