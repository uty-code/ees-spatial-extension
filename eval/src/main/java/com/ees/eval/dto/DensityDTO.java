package com.ees.eval.dto;

import com.ees.eval.domain.RegionType;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DensityDTO {
    private Long branchId;
    private RegionType regionType;
    private Integer radius;
    private Integer nearbySameBrandCount;
    private String densityLevel; // LOW, MID, HIGH
    private BigDecimal difficultyCoefficient;
    private Double riskRatio;
}
