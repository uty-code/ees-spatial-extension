package com.ees.eval.dto;

import lombok.Builder;

/**
 * 평가자 매핑 정합성 검사 시 발견된 예외 상황(Anomaly)을 담는 레코드입니다.
 * 
 * @param evaluateeId 피평가자 사번
 * @param evaluateeName 피평가자 이름
 * @param deptName 피평가자 부서명
 * @param anomalyType 위반 유형 (예: MISSING_SELF, RETIRED_EVALUATOR 등)
 * @param description 위반 상세 설명
 * @param severity 위험도 (ERROR, WARNING)
 */
@Builder
public record MappingAnomalyDTO(
        Long evaluateeId,
        String evaluateeName,
        String deptName,
        String anomalyType,
        String description,
        String severity
) {
}
