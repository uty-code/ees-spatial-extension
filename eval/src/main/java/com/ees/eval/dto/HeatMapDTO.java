package com.ees.eval.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeatMapDTO {
    private Long branchId;
    private String branchName;
    private com.ees.eval.domain.RegionType regionType;
    private Double lat;
    private Double lng;
    private String densityLevel;
    private BigDecimal score;
    private String riskLevel;
    private Long empId; // 추가: 사원 ID (매핑용)
}
