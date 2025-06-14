package org.univcabi.univcabi.cabinet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.univcabi.univcabi.user.entity.User;


import java.time.LocalDateTime;

@Entity
@Table(name = "cabinets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Log4j2
@EntityListeners(AuditingEntityListener.class) // 엔터티 리스너(해당 엔티티에 감사 기능을 사용하겠다고 선언)를 통하여 @CreatedDate와 @LastModifiedDate의 자동 시간 기록
public class Cabinet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name="building_id", nullable = false)
    private Building buildingId;

    @ManyToOne(optional = true)
    @JoinColumn(name="user_id")
    private User userId;

    @NotNull
    @Column(name="cabinet_number", nullable = false)
    private String cabinetNumber;

    @Enumerated(EnumType.STRING)
    private CabinetStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder(toBuilder = true)
    private Cabinet(Long id, Building buildingId, User userId, String cabinetNumber, CabinetStatus status, LocalDateTime paidAt, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.buildingId = buildingId;
        this.userId = userId;
        this.cabinetNumber = cabinetNumber;
        this.status = status;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    // 영속성 객체인 Cabinet 의 상태를 AVAILABLE로 바꾸는 메서드 (리뷰 부탁드리옵니다);;
    public void replaceStatusToAVAILVABLE(){
        this.status = CabinetStatus.AVAILABLE;
    }

    // 영속성 객체인 Cabinet 의 상태를 USING로 바꾸는 메서드 (리뷰 부탁드리옵니다);;
    public void replaceStatusToUSING(){
        this.status = CabinetStatus.USING;
    }

    // 영속성 객체인 Cabinet 의 상태를 BROKEN로 바꾸는 메서드 (리뷰 부탁드리옵니다);;
    public void replaceStatusToBROKEN(){
        this.status = CabinetStatus.BROKEN;
    }

    // 영속성 객체인 Cabinet 의 상태를 OVERDUE로 바꾸는 메서드 (리뷰 부탁드리옵니다);;
    public void replaceStatusToOVERDUE(){
        this.status = CabinetStatus.OVERDUE;
    }

    public void setUser(User user){
        this.userId = user;
    }
}
