package org.univcabi.univcabi.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.univcabi.univcabi.auth.entity.Authn;

import java.util.Optional;

@Repository
public interface AuthnRepository extends JpaRepository<Authn, Long> {

    // 조회
    Optional<Authn> findByStudentNumber(String studentNumber);

    // 해당 학번 회원 존재 유무 판단
    boolean existsByStudentNumber(String studentNumber);


}
