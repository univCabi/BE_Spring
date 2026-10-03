package org.univcabi.univcabi.cabinet.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetBookmark;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.repository.CabinetBookmarkRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkAuthVo;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkListVo;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.support.HibernateQueryCounter;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * getBookmarkList()에서 의심되는 Bookmark -> Cabinet -> Building, Cabinet -> User -> Authn
 * N+1을 Hibernate Statistics로 직접 측정한다.
 * 북마크 개수(N)를 늘려도 실행되는 쿼리 수가 늘어나지 않아야 한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({CabinetBookmarkService.class, QueryDslConfig.class})
class CabinetBookmarkServiceSqlIntegrationTest {

    @Autowired CabinetBookmarkService cabinetBookmarkService;
    @Autowired UserRepository userRepository;
    @Autowired AuthnRepository authnRepository;
    @Autowired CabinetRepository cabinetRepository;
    @Autowired CabinetBookmarkRepository cabinetBookmarkRepository;
    @Autowired EntityManager entityManager;

    @Test
    void getBookmarkList_queryCountDoesNotGrowWithBookmarkCount() {
        int bookmarkCount = 18;

        User requester = userRepository.save(User.builder()
                .name("북마크 조회 사용자")
                .affiliation("컴퓨터공학과")
                .isVisible(true)
                .build());
        authnRepository.save(Authn.builder()
                .studentNumber("202700000")
                .password("test-password")
                .role(AuthnRole.NORMAL)
                .user(requester)
                .build());

        for (int i = 0; i < bookmarkCount; i++) {
            Building building = Building.builder()
                    .name(BuildingName.공학1관)
                    .floor(i + 1)
                    .section("A")
                    .width(10)
                    .height(10)
                    .build();
            entityManager.persist(building);

            // 각 캐비닛마다 소유자를 다르게 두어 Cabinet -> User -> Authn 체인을 재현한다.
            User owner = userRepository.save(User.builder()
                    .name("캐비닛 소유자 " + i)
                    .affiliation("컴퓨터공학과")
                    .isVisible(true)
                    .build());
            authnRepository.save(Authn.builder()
                    .studentNumber(String.valueOf(202700100 + i))
                    .password("test-password")
                    .role(AuthnRole.NORMAL)
                    .user(owner)
                    .build());

            Cabinet cabinet = Cabinet.builder()
                    .buildingId(building)
                    .userId(owner)
                    .cabinetNumber("A" + (i + 1))
                    .status(CabinetStatus.USING)
                    .build();
            cabinetRepository.save(cabinet);

            cabinetBookmarkRepository.save(CabinetBookmark.builder()
                    .user(requester)
                    .cabinet(cabinet)
                    .build());
        }

        entityManager.flush();
        entityManager.clear();

        HibernateQueryCounter queryCounter = new HibernateQueryCounter(entityManager);
        queryCounter.reset();

        List<CabinetBookmarkListVo> result =
                cabinetBookmarkService.getBookmarkList(new CabinetBookmarkAuthVo("202700000"));

        long queryCount = queryCounter.count();
        System.out.println("=== getBookmarkList executed SQL count (bookmarks=" + bookmarkCount + "): " + queryCount + " ===");

        assertThat(result).hasSize(bookmarkCount);
        // 북마크 조회 사용자 조회(1) + 북마크와 캐비닛/빌딩 fetch join 조회(1) = 2건 고정이어야 한다.
        // N+1이 남아있다면 이 값은 bookmarkCount에 비례해 커진다(수정 전에는 8개 기준 10건이었다).
        assertThat(queryCount).isEqualTo(2);
    }
}
