package org.univcabi.univcabi.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.univcabi.univcabi.auth.entity.Authn;

import java.util.Optional;

@Repository
public interface AuthnRepository extends JpaRepository<Authn, Long> {

    // 조회
    Optional<Authn> findByStudentNumber(String studentNumber);

    // 삭제
    void deleteByStudentNumber(String studentNumber);

    // 생성의 save 는 JPA 제공
}
