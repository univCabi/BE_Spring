package org.univcabi.univcabi.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;
import org.univcabi.univcabi.user.vo.RentCabinetInfoVo;
import org.univcabi.univcabi.user.vo.UserProfileVo;

import java.util.logging.ErrorManager;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileVo getUserProfileByStudentNumber(String studentNumber) {
        // 유저 정보 가져오기
        User user = userRepository.findUserByStudentNumber(studentNumber)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다."));

        // 해당 유저의 cabinetHistory 조회 (지금 빌리고 있는 것)
        RentCabinetInfoVo rentCabinetInfoVo = userRepository
                .getLatestCabinetHistoryByStudentNumber(studentNumber)
                .map(history -> {
                    Cabinet cabinet = history.getCabinet();
                    Building building = cabinet.getBuildingId();

                    return new RentCabinetInfoVo(
                            building.getName().name(),
                            building.getFloor(),
                            Integer.parseInt(cabinet.getCabinetNumber()),
                            cabinet.getStatus().name(),
                            history.getCreatedAt(),
                            history.getExpiredAt()
                    );
                })
                .orElse(null);

        return new UserProfileVo(
                user.getName(),
                user.getIsVisible(),
                user.getAffiliation(),
                studentNumber,
                user.getPhoneNumber(),
                rentCabinetInfoVo
        );
    }
}
