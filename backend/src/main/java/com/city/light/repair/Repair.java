package com.city.light.repair;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 报修工单：针对「故障」灯具开单，完工结论回写灯具状态（已修复→亮灯，无法修复→报废）。 */
@Data
@Entity
@Table(name = "repair")
public class Repair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long lampId;

    private String faultType;

    private LocalDate reportDate;

    private String worker;

    private String status;

    private LocalDate finishDate;

    private BigDecimal cost;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
