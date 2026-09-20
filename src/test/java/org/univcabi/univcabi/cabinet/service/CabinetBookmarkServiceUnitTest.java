package org.univcabi.univcabi.cabinet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetBookmark;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.repository.CabinetBookmarkRepository;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkVo;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CabinetBookmarkServiceUnitTest {
    @Mock
    CabinetBookmarkRepository cabinetBookmarkRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CabinetRepository cabinetRepository;

    @InjectMocks
    private CabinetBookmarkService cabinetBookmarkService;

    @Test
    void RemoveBookmarkByCabinetIdTest() {
        CabinetBookmarkVo cabinetBookmarkVo = new CabinetBookmarkVo(1L, "202213185");
        User user = User.builder()
                .id(1L)
                .name("테스트 사용자")
                .affiliation("컴퓨터공학과")
                .isVisible(true)
                .build();

        Cabinet cabinet = Cabinet.builder()
                .id(1L)
                .cabinetNumber("101")
                .status(CabinetStatus.AVAILABLE)
                .build();

        CabinetBookmark cabinetBookmark = CabinetBookmark.builder()
                .id(1L)
                .user(user)
                .cabinet(cabinet)
                .build();

        given(userRepository.findUserByStudentNumber(cabinetBookmarkVo.studentNumber())).willReturn(Optional.of(user));
        given(cabinetRepository.findById(cabinetBookmarkVo.cabinetId())).willReturn(Optional.of(cabinet));
        given(cabinetBookmarkRepository.findByUserAndCabinetAndDeletedAtIsNull(user,cabinet)).willReturn(Optional.of(cabinetBookmark));

        cabinetBookmarkService.removeBookmarkByCabinetId(cabinetBookmarkVo);

        assertThat(cabinetBookmark.getDeletedAt()).isNotNull();
    }
}
