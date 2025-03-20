package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.univcabi.univcabi.cabinet.dto.request.CabinetReturnRequestDto;
import org.univcabi.univcabi.cabinet.dto.request.CabinetSearchDetailRequestDto;
import org.univcabi.univcabi.cabinet.dto.request.CabinetRentHistoryRequestDto;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.vo.CabinetRentVo;
import org.univcabi.univcabi.cabinet.vo.CabinetReturnVo;

import java.util.List;
import java.util.Optional;

public interface CabinetRepository extends JpaRepository<Cabinet, Long>, CabinetCustomRepository {

    // 페이징된 결과
    Page<Cabinet> findAllCabinetInfo(Pageable pageable);

    // 단일 엔티티 조회
    Optional<Cabinet> findCabinetById(Long cabinetId);

    // 키워드 검색
    @Query("SELECT c FROM Cabinet c JOIN c.buildingId b WHERE " +
            "c.cabinetNumber LIKE %:keyword% OR " +
            "CAST(b.name AS string) LIKE %:keyword%")
    List<Cabinet> searchCabinetByKeyword(@Param("keyword") String keyword, Pageable pageable);


    // 상세 검색
    Optional<List<Cabinet>> findAllCabinetInfo(CabinetSearchDetailRequestDto requestDto);

    // 대여 이력 조회
    Optional<List<Cabinet>> findAllCabinetInfo(CabinetRentHistoryRequestDto requestDto);

    // 대여 이력 조회 - 학번으로
    List<CabinetRentHistory> findRentHistoryByStudentNumber(String studentNumber);

    // 대여 처리
    Cabinet rentCabinetByCabinetId(CabinetRentVo requestVo);

    // 반납 처리
    Cabinet returnCabinet(CabinetReturnVo requestVo);
}