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

    @Column(name = "reason")
    private String reason;

    @Builder(toBuilder = true)
    private Cabinet(Long id,
                    Building buildingId,
                    User userId,
                    String cabinetNumber,
                    CabinetStatus status,
                    LocalDateTime paidAt,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt,
                    LocalDateTime deletedAt,
                    String reason) {
        this.id = id;
        this.buildingId = buildingId;
        this.userId = userId;
        this.cabinetNumber = cabinetNumber;
        this.status = status;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.reason=reason;
    }

    // 영속성 객체인 Cabinet 의 상태를 Status 매개변수로 변경하는 메서드
    public void setStatus(CabinetStatus status){
        this.status= status;
        if(status==CabinetStatus.AVAILABLE) {
            this.reason=null;
        }
    }
    // status가 BROKEN일 때 사용되는 메서드 오버로딩 (reason)파라미터 값을 받음
    public void setStatus(CabinetStatus status, String reason){
        this.status=status;
        this.reason = reason;
    }


    public void setUser(User user){
        this.userId = user;
    }
}
