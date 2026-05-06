package com.ees.eval.dto;

import lombok.Builder;

/**
 * 최종 등급 확정 목록에서 개별 평가 대상자의 상태를 나타내는 DTO입니다.
 * N+1 쿼리 방지를 위해 모든 필요 플래그를 미리 계산하여 담습니다.
 *
 * @param mappingId      EXECUTIVE 관계의 매핑 ID
 * @param evaluateeId    피평가자 사번
 * @param evaluateeName  피평가자 성명
 * @param deptName       부서명
 * @param allSubmitted   해당 피평가자에 대한 모든 요소(성과/역량) 평가 완료 여부
 * @param selfSubmitted  피평가자 본인의 자가평가 제출 여부
 * @param weightValid    피평가자 소속 부서의 가중치 설정 유효성 여부
 */
@Builder
public record FinalGradeTaskDTO(
        Long mappingId,
        Long evaluateeId,
        String evaluateeName,
        String deptName,
        boolean allSubmitted,
        boolean selfSubmitted,
        boolean weightValid
) {
}
