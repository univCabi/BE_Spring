package org.univcabi.univcabi.auth.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.auth.vo.AuthnDeleteVo;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({AuthnService.class, QueryDslConfig.class})
class AuthnServiceIntegrationTest {

    @Autowired AuthnService authnService;
    @Autowired AuthnRepository authnRepository;
    @Autowired EntityManager entityManager;

    @Test
    void softDeleteUserByStudentNumber_persistsDeletedAt() {
        String studentNumber = "202600001";
        User user = User.builder()
                .name("탈퇴 테스트 사용자")
                .affiliation("컴퓨터공학과")
                .isVisible(true)
                .build();
        entityManager.persist(user);

        entityManager.persist(Authn.builder()
                .studentNumber(studentNumber)
                .password("test-password")
                .role(AuthnRole.NORMAL)
                .user(user)
                .build());
        entityManager.flush();
        entityManager.clear();

        authnService.softDeleteUserByStudentNumber(new AuthnDeleteVo(studentNumber));

        entityManager.flush();
        entityManager.clear();

        Authn reloaded = authnRepository.findByStudentNumber(studentNumber).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isNotNull();
    }
}
