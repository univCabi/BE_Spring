package org.univcabi.univcabi.cabinet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Entity
@Table(name = "cabinet_positions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Log4j2
public class CabinetPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name="cabinet_id", unique = true)
    private Cabinet cabinetId;

    @Column(name="cabinet_x_pos")
    private String cabinetXPos;

    @Column(name="cabinet_y_pos")
    private String cabinetYPos;
}
