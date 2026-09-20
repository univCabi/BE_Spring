package org.univcabi.univcabi.cabinet.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkVo;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetBookmark;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.repository.CabinetBookmarkRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CabinetBookmarkService.class, QueryDslConfig.class})
public class CabinetBookmarkServiceIntegrationTest {
    private final CabinetBookmarkService cabinetBookmarkService;
    private final UserRepository userRepository;
    private final CabinetRepository cabinetRepository;
    private final CabinetBookmarkRepository cabinetBookmarkRepository;
    private final AuthnRepository authnRepository;
    private final EntityManager entityManager;

    @Autowired
    public CabinetBookmarkServiceIntegrationTest(CabinetBookmarkService cabinetBookmarkService, UserRepository userRepository, CabinetRepository cabinetRepository, CabinetBookmarkRepository cabinetBookmarkRepository, AuthnRepository authnRepository, EntityManager entityManager) {
        this.cabinetBookmarkService = cabinetBookmarkService;
        this.userRepository = userRepository;
        this.cabinetRepository = cabinetRepository;
        this.cabinetBookmarkRepository = cabinetBookmarkRepository;
        this.authnRepository = authnRepository;
        this.entityManager = entityManager;
    }

    @Test
    void RemoveBookmarkByCabinetIdTest() {
        User user = User.builder()
                .name("테스트 사용자")
                .affiliation("컴퓨터공학과")
                .isVisible(true)
                .build();
        user = userRepository.save(user);
        Authn authn = Authn.builder()
                .studentNumber("202213185")
                .password("test-password")
                .role(AuthnRole.NORMAL)
                .user(user)
                .build();
        authnRepository.save(authn);

        Building building = Building.builder()
                .name(BuildingName.공학1관)
                .floor(1)
                .section("A")
                .width(10)
                .height(10)
                .build();
        entityManager.persist(building);

        Cabinet cabinet = Cabinet.builder()
                .buildingId(building)
                .cabinetNumber("101")
                .status(CabinetStatus.AVAILABLE)
                .build();
        cabinetRepository.save(cabinet);
        CabinetBookmark cabinetBookmark = CabinetBookmark.builder()
                .user(user)
                .cabinet(cabinet)
                .build();
        cabinetBookmarkRepository.save(cabinetBookmark);

        CabinetBookmarkVo cabinetBookmarkVo = new CabinetBookmarkVo(cabinet.getId(),"202213185");

        cabinetBookmarkService.removeBookmarkByCabinetId(cabinetBookmarkVo);

        entityManager.flush();
        entityManager.clear();

        Optional<CabinetBookmark> returnedCabinetBookmark = cabinetBookmarkRepository.findById(cabinetBookmark.getId());
        assertThat(returnedCabinetBookmark.orElseThrow().getDeletedAt()).isNotNull();
    }
}
