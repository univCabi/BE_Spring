package org.univcabi.univcabi.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name="authns")
@Getter // 자동 getter 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Authn {

    @Id  // 기본키
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB에서 직접 ID 생성
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String studentNumber;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuthnRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime deletedAt =null;

    @PrePersist // 자동 현재 시간 저장
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate // 자동 현재시간 업데이트
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Authn(String studentNumber, String password, AuthnRole role, LocalDateTime deletedAt) {
        this.studentNumber = studentNumber;
        this.password = password;
        this.role = role;
        this.deletedAt = deletedAt;
    }
}
