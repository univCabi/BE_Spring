package org.univcabi.univcabi.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.univcabi.univcabi.cabinet.entity.Building;

@Entity
@Table(name = "users")  // DB의 'users' 테이블과 매핑
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Log4j2
@Getter
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(nullable = false, length = 10)
    private String name;

    @Enumerated(EnumType.STRING)
    private Building building;

    @NotNull
    @Column(name="is_visible")
    private Boolean isVisible;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @Builder(toBuilder = true)
    private User(Long id, String name, Building building, Boolean isVisible, LocalDateTime createdAt, LocalDateTime updatedAt,  LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.isVisible = isVisible;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
        this.deletedAt = deletedAt;
    }
}