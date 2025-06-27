package org.univcabi.univcabi.cabinet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;
import org.univcabi.univcabi.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "cabinet_histories")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CabinetHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabinet_id")
    private Cabinet cabinet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 히스토리 만료 시각을 정하는 메서드
    @Setter // 세터를 사용하여 expiredAt 파라미터 값 변경
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 히스토리 반납 시각을 정하는 메서드
    @Setter
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Methods for cabinet rent and return
    public static CabinetHistory createRentHistory(User user, Cabinet cabinet, LocalDateTime expiredAt) {
        return CabinetHistory.builder()
                .user(user)
                .cabinet(cabinet)
                .expiredAt(expiredAt)
                .build();
    }

}