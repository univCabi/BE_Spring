package org.univcabi.univcabi.cabinet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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