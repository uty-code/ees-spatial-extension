package com.ees.eval.dto;

import lombok.Builder;

/**
 * 평가 결과 현황 화면에서 사용하는 DTO입니다.
 * 피평가자의 종합 점수, 등급, 유형별 점수를 포함합니다.
 *
 * @param empId             사원 ID
 * @param empName           사원명
 * @param deptName          부서명
 * @param positionName      직급명
 * @param performanceScore      성과/업무 평가 환산 점수 (0~100)
 * @param competencyScore       역량 평가 환산 점수 (0~100)
 * @param multiDimensionalScore 다면 평가 환산 점수 (0~100)
 * @param totalScore            종합 점수 (가중 합산, 0~100)
 * @param gradeCode             절대평가 등급 코드 (S/A/B/C/D)
 * @param isConfirmed           최종 확정 여부 (final_grades_51 존재 여부)
 */
@Builder
public record EvaluationResultDTO(
        Long empId,
        String empName,
        String deptName,
        String positionName,
        Integer performanceScore,
        Integer competencyScore,
        Integer multiDimensionalScore,
        Integer totalScore,
        String gradeCode,
        boolean isConfirmed
) {
}
