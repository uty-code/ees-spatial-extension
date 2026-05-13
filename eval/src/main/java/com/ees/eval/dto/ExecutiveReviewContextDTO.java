package com.ees.eval.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutiveReviewContextDTO {
    private Long empId;
    private String empName;
    private String branchName; // 추가: 지점명
    private BigDecimal finalScore;
    private BigDecimal baseScore; // 공간 보정 전 원본 점수
    private String riskLevel; // LOW, MEDIUM, HIGH
    private BigDecimal gap;
    private boolean snapshotAvailable;
    
    // Spatial Context
    private String densityLevel;
    private Integer nearbySameBrandCount;
    private BigDecimal difficultyCoefficient;
    private boolean spatialAdjusted;
    
    private Map<String, BigDecimal> trends; // e.g., {"2023-Q4": 75.0, "2024-Q1": 82.5}
}
