package com.ees.eval.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationGapDTO {
    private Long empId;
    private String empName;
    private String branchName;     // 추가: 지점명
    private BigDecimal baseScore;  // 추가: 운영 점수 (프론트 엔드 매핑용)
    private BigDecimal finalScore; // 추가: 최종 점수 (프론트 엔드 매핑용)
    private BigDecimal operationalScore; // 기존 필드 유지
    private BigDecimal managerScore;     // 기존 필드 유지
    private BigDecimal gap;
    private String status; // NORMAL, WARNING, ANOMALY, PENDING
    private Long periodId;
    private Long evaluationId;     // 추가: 평가 ID (스냅샷 조회용)
}
