package com.city.light.lamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 灯具：挂在路段灯杆上，一只灯具对应一个杆号，状态决定能否报修。 */
@Data
@Entity
@Table(name = "lamp")
public class Lamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String lampNo;

    private Long roadId;

    private String poleNo;

    private String type;

    private Integer watt;

    private LocalDate installDate;

    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
