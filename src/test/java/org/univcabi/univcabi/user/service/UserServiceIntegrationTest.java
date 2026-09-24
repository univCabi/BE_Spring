package org.univcabi.univcabi.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.repository.AuthnRepository;
import org.univcabi.univcabi.configs.QueryDslConfig;
import org.univcabi.univcabi.user.repository.UserRepository;
import org.univcabi.univcabi.user.vo.AdminUserCreateVo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest
@Import({UserService.class, QueryDslConfig.class})
class UserServiceIntegrationTest {
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @MockitoSpyBean AuthnRepository authnRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void createAdminUser_rollsBackUserWhenExceptionOccursBeforeAuthnSave() {
        long beforeCount = userRepository.count();
        AdminUserCreateVo request = spy(new AdminUserCreateVo(
                "롤백 테스트", "컴퓨터공학과", "01012345678",
                "202213185", "test-password", AuthnRole.ADMIN,
                null, null, null));
        RuntimeException failure = new IllegalStateException("사용자 저장 후 강제 예외");

        // 실제 사용자 INSERT를 확인한 뒤, 인증 정보 생성 단계에서 예외를 발생시킨다.
        doAnswer(invocation -> {
            assertThat(userRepository.count()).isEqualTo(beforeCount + 1);
            throw failure;
        }).when(request).studentNumber();

        assertThatThrownBy(() -> userService.createAdminUser(request))
                .isSameAs(failure);

        verifyNoInteractions(authnRepository);
        // 테스트 트랜잭션이 없으므로 서비스 롤백 완료 후 새로운 조회로 확인한다.
        assertThat(userRepository.count()).isEqualTo(beforeCount);
    }
}
