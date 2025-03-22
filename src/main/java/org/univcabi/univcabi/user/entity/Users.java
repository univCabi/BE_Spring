package org.univcabi.univcabi.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private Long buildingId;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, length = 50)
    private String affiliation;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private Boolean isVisible;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){this.updatedAt = LocalDateTime.now();}


    @Builder
    public Users(Long buildingId, String name, String affiliation, String phoneNumber, Boolean isVisible, LocalDateTime deletedAt) {
        this.buildingId = buildingId;
        this.name = name;
        this.affiliation = affiliation;
        this.phoneNumber = phoneNumber;
        this.isVisible = isVisible;
        this.deletedAt = deletedAt;
    }
}
