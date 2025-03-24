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

    private Integer floor;

    private String section;

    private Integer width;

    private Integer height;
}