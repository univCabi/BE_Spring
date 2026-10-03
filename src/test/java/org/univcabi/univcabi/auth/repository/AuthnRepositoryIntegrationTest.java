package org.univcabi.univcabi.auth.repository;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.support.HibernateQueryCounter;
import org.univcabi.univcabi.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
class AuthnRepositoryIntegrationTest {
    @Autowired EntityManager entityManager;
    @Autowired AuthnRepository authnRepository;

    @Test
    void findWithUserByStudentNumber_fetchesUserWithoutAdditionalQueries() {
        User user = User.builder().name("조회 테스트 사용자")
                .affiliation("컴퓨터공학과").isVisible(true).build();
        entityManager.persist(user);
        entityManager.persist(Authn.builder().studentNumber("202600004")
                .password("test-password").role(AuthnRole.NORMAL).user(user).build());
        entityManager.flush();
        entityManager.clear();

        HibernateQueryCounter counter = new HibernateQueryCounter(entityManager);
        counter.reset();
        Authn result = authnRepository.findWithUserByStudentNumber("202600004").orElseThrow();

        assertThat(Hibernate.isInitialized(result.getUser())).isTrue();
        assertThat(counter.count()).isEqualTo(1);
        // 조회 후 분리된 엔티티에서도 사용자 정보를 읽을 수 있어야 한다.
        entityManager.clear();
        assertThat(result.getUser().getId()).isEqualTo(user.getId());
        assertThat(result.getUser().getName()).isEqualTo("조회 테스트 사용자");
        assertThat(result.getUser().getIsVisible()).isTrue();
        assertThat(counter.count()).isEqualTo(1);
    }
}
