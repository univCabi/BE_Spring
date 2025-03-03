package org.univcabi.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.auth.entity.Authn;

import java.util.Optional;

public interface AuthnRepository extends JpaRepository<Authn, Long> {

    // 조회
    Optional<Authn> findByStudentNumber(String studentNumber);

    // 삭제
    void deleteByStudentNumber(String studentNumber);

    // 생성의 save 는 JPA 제공
}
