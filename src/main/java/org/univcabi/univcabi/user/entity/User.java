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
import org.univcabi.univcabi.auth.entity.Authn;
import org.univcabi.univcabi.cabinet.entity.Building;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Log4j2
@Getter
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false)
    private String affiliation;

    @Column(name="phone_number")
    private String phoneNumber;

    @ManyToOne
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;  // 이것만 유지하고 다른 building 관련 필드는 제거

    @NotNull
    @Column(name="is_visible")
    private Boolean isVisible;

    // User 엔티티는 관계의 주인이 아님 (mappedBy 사용)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Authn authn;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @Builder(toBuilder = true)
    private User(Long id, String name, String affiliation, String phoneNumber, Building building, Boolean isVisible, Authn authn, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.affiliation = affiliation;
        this.phoneNumber = phoneNumber;
        this.building = building;
        this.isVisible = isVisible;
        this.authn = authn;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
        this.deletedAt = deletedAt;
    }
}