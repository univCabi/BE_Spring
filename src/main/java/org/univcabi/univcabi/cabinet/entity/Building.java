package org.univcabi.univcabi.cabinet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;

@Entity
@Table(name = "buildings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Log4j2
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private BuildingName name;

    private String floor;

    private Integer width;

    private Integer height;
}


//package org.clubs.blueheart.activity.domain;
//
//import jakarta.persistence.*;
//        import jakarta.validation.constraints.*;
//        import lombok.AccessLevel;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.extern.log4j.Log4j2;
//
//import java.time.LocalDateTime;
//import java.time.OffsetDateTime;
//
//import org.clubs.blueheart.user.domain.User;
//import org.clubs.blueheart.user.domain.UserRole;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedDate;
//
//@Entity
//@Table(name = "activities")  // DB의 'users' 테이블과 매핑
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@Getter
//@Log4j2
//public class Activity {
//
//    // ManyToMany 관계 설정
//    @ManyToMany
//    @JoinTable(
//            name = "activity_histories",               // 중간 테이블 이름
//            joinColumns = @JoinColumn(name = "activity_id"),   // groups 테이블 FK
//            inverseJoinColumns = @JoinColumn(name = "user_id") // users 테이블 FK
//    )
//    private final java.util.Set<User> users = new java.util.HashSet<>();
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @NotNull
//    @ManyToOne(optional = false)
//    @JoinColumn(name="creator_id", nullable = false)
//    private User creatorId;
//
//    @NotNull
//    @Column(nullable = false, length = 15)
//    private String title;
//
//    @Enumerated(EnumType.STRING)
//    private ActivityStatus status;
//
//    @Column(name="max_number")
//    private Integer maxNumber;
//
//    @NotNull
//    @Column(nullable = false, length = 255)
//    private String description;
//
//    @NotNull
//    @Column(nullable = false, length = 255)
//    private String place;
//
//    @NotNull
//    @Column(name="place_url", nullable = true, length = 255)
//    private String placeUrl;
//
//    @NotNull
//    @Column(name = "expired_at", nullable = false)
//    private LocalDateTime expiredAt;
//
//    @CreatedDate
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "deleted_at")
//    private LocalDateTime deletedAt;
//
//    @Builder(toBuilder = true)
//    public Activity(Long id, User creatorId, String title,  ActivityStatus status, Integer maxNumber, String description, String place, String placeUrl, LocalDateTime createdAt, LocalDateTime expiredAt, LocalDateTime deletedAt) {
//        this.id = id;
//        this.creatorId = creatorId;
//        this.title = title;
//        this.status = status;
//        this.maxNumber = maxNumber;
//        this.description = description;
//        this.place = place;
//        this.placeUrl = placeUrl;
//        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now(); // 기본값 설정
//        this.expiredAt = expiredAt;
//        this.deletedAt = deletedAt;
//    }
//
//    @Builder(toBuilder = true)
//    public Activity(Long id, User creatorId, String title, Integer maxNumber, String description, String place, String placeUrl, LocalDateTime createdAt, LocalDateTime expiredAt, LocalDateTime deletedAt) {
//        this.id = id;
//        this.creatorId = creatorId;
//        this.title = title;
//        this.status = ActivityStatus.PROGRESSING;
//        this.maxNumber = maxNumber;
//        this.description = description;
//        this.place = place;
//        this.placeUrl = placeUrl;
//        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now(); // 기본값 설정
//        this.expiredAt = expiredAt;
//        this.deletedAt = deletedAt;
//    }
//
//    public Activity updatedActivityFields(String title, ActivityStatus status, Integer maxNumber, String description, String place, String placeUrl, LocalDateTime expiredAt) {
//        return this.toBuilder()
//                .title(title != null ? title : this.title)
//                .status(status != null ? status : this.status)
//                .maxNumber(maxNumber != null ? maxNumber : this.maxNumber)
//                .description(description != null ? description : this.description)
//                .place(place != null ? place : this.place)
//                .placeUrl(placeUrl != null ? placeUrl : this.placeUrl)
//                .expiredAt(expiredAt != null ? expiredAt : this.expiredAt)
//                .build();
//    }
//}