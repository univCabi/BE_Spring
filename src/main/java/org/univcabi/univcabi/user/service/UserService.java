package org.univcabi.univcabi.user.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.exception.ServiceException;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.user.repository.UserRepository;
import org.univcabi.univcabi.user.vo.RentCabinetInfoVo;
import org.univcabi.univcabi.user.vo.UserProfileVo;
import org.univcabi.univcabi.user.vo.UserVisibilityVo;
import org.springframework.core.io.ResourceLoader;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ErrorManager;

import static org.univcabi.univcabi.exception.ExceptionStatus.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final ResourceLoader resourceLoader;

    public UserProfileVo getUserProfileByStudentNumber(String studentNumber) {
        // 유저 정보 가져오기
        User user = userRepository.findUserByStudentNumber(studentNumber)
                .orElseThrow(() -> new ServiceException(USER_NOT_FOUND));

        // 해당 유저의 cabinetHistory 조회 (지금 빌리고 있는 것)
        // rentCabinetInfo 에 필요한 정보 조회
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

    // User정보의 isVisible 파라미터 수정
    @Transactional
    public void updateUserVisibility(UserVisibilityVo requestVo){
        if(userRepository.updateUserVisibilityByStudentNumber(
                requestVo.studentNumber(),requestVo.isVisible())==0){
            throw new ServiceException(USER_VISIBILITY_UPDATE_FAILED);
        }
    }

    // 데이터 베이스 초기화
    @Transactional
    public void resetDatabase(){
        List<String> scripts = List.of(
                "classpath:db/migration/V1__Flush_all_data.sql",
                "classpath:db/migration/V2__Insert_building_data.sql",
                "classpath:db/migration/V3__Insert_user_data.sql",
                "classpath:db/migration/V4__Insert_authn_data.sql",
                "classpath:db/migration/V5__Insert_cabinet_data.sql",
                "classpath:db/migration/V6__Insert_cabinet_position_data.sql",
                "classpath:db/migration/V7__Insert_playable_data.sql"
        );

        for (String path : scripts){
            String sql = loadSql(path);
            for(String query : sql.split(";")){
                if(!query.trim().isEmpty()){
                    entityManager.createNativeQuery(query).executeUpdate();
                }
            }
        }
    }

    // sql 파일 경로를 읽고 String 으로 반환해주는 메서드
    private String loadSql(String path){
        Resource resource = resourceLoader.getResource(path);
        try(InputStream input = resource.getInputStream()){
            String sql = new String(input.readAllBytes());
            return Arrays.stream(sql.split("\n"))
                    .map(String::trim) // 양쪽 공백 제거
                    .filter(line->!line.startsWith("--")&&!line.isEmpty()) // 주석, 빈줄 제거
                    .reduce((str1,str2)->str1+"\n"+str2)
                    .orElse("");
        }
        catch (IOException e) {
            throw new ServiceException(SQL_FILE_LOAD_FAILED);
        }
    }
}
