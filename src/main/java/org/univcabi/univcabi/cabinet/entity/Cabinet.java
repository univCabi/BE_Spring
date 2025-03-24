package org.univcabi.univcabi.cabinet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.univcabi.univcabi.user.entity.User;


import java.time.LocalDateTime;

@Entity
@Table(name = "cabinets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Log4j2
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

}
