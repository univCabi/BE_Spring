package org.univcabi.univcabi.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.univcabi.univcabi.user.entity.User;

import java.time.LocalDateTime;


@Entity
@Table(name="authns")
@Getter // 자동 getter 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // 생성자 자동 생성
@Builder
public class Authn {

    @Id  // 기본키
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB에서 직접 ID 생성
    private Long id;

    @Column(name="student_number",nullable = false, unique = true, length = 50)
    private String studentNumber;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuthnRole role;

    // Authn 엔티티가 관계의 주인이 됨 (외래 키를 가짐)
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)  // user_id를 외래 키로 설정
    private User user;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name="deleted_at")
    @Builder.Default
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

}
