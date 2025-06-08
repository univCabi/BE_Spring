package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.univcabi.univcabi.cabinet.entity.Cabinet;

import java.util.List;

public interface CabinetRepository extends JpaRepository<Cabinet, Long>, CabinetCustomRepository {

    // 페이징된 결과
    @Query("SELECT c FROM Cabinet c JOIN FETCH c.buildingId")
    Page<Cabinet> findAllCabinetInfo(Pageable pageable);

    // 키워드 검색
    @Query("SELECT c FROM Cabinet c JOIN c.buildingId b WHERE " +
            "c.cabinetNumber LIKE %:keyword% OR " +
            "CAST(b.name AS string) LIKE %:keyword%")
    Page<Cabinet> searchCabinetsByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Cabinet c JOIN c.buildingId b WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "c.cabinetNumber LIKE CONCAT('%', :keyword, '%') OR " +
            "CAST(b.name AS string) LIKE CONCAT('%', :keyword, '%') OR " +
            "CAST(b.floor AS string) LIKE CONCAT('%', :keyword, '%'))")
    Page<Cabinet> findAllCabinetInfoByKeyword(@Param("keyword") String keyword, Pageable pageable);
}