package org.univcabi.univcabi.cabinet.repository;

import org.clubs.blueheart.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.univcabi.cabinet.entity.Cabinet;

import java.util.List;
import java.util.Optional;

//TODO: 이거 설명 하기 queryDSL + JPA 혼용
//TODO: https://jojoldu.tistory.com/372
public interface CabinetRepository extends JpaRepository<Cabinet, Long> {

    Optional<List<Cabinet>> findAllCabinetInfo(CabinetFindAllInfoRequestDto cabinetFindAllInfoRequestDto);
    Optional<List<Cabinet>> findAllCabinetInfo(CabinetFindOneInfoRequestDto cabinetFindOneInfoRequestDto);
    Optional<List<Cabinet>> findAllCabinetInfo(CabinetRentRequestDto cabinetRentRequestDto);
    Optional<List<Cabinet>> findAllCabinetInfo(CabinetReturnRequestDto cabinetReturnRequestDto);
    Optional<List<Cabinet>> findAllCabinetInfo(CabinetSearchDetailRequestDto cabinetSearchDetailRequestDto);
    Optional<List<Cabinet>> findAllCabinetInfo(CabinetRentHistoryRequestDto cabinetRentHistoryRequestDto);


//    Optional<List<User>> findUsersByUsernameContainsAndDeletedAtIsNull(String userName);  // JPA Query Method
//    Optional<List<User>> findUsersByStudentNumberStartsWithAndDeletedAtIsNull(String studentNumber); // JPA Query Method
//    Optional<User> findUserByIdAndDeletedAtIsNull(Long id);
//    Boolean existsByStudentNumberAndDeletedAtIsNull(String studentNumber);
//
//    Optional<List<User>> findAllByDeletedAtIsNull();
//
//    Optional<User> findOneUserByStudentNumberAndUsername(String studentNumber, String username);

}

