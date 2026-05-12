package com.ees.eval.service;

/**
 * 평가 점수 산출 및 절대평가 등급 매핑을 담당하는 서비스 인터페이스입니다.
 *
 * <p>등급 기준 (절대평가):</p>
 * <ul>
 *     <li>S: 95 ~ 100점 (탁월)</li>
 *     <li>A: 85 ~ 94점 (우수)</li>
 *     <li>B: 75 ~ 84점 (양호)</li>
 *     <li>C: 60 ~ 74점 (보통)</li>
 *     <li>D: 0 ~ 59점 (미흡)</li>
 * </ul>
 */
public interface ScoreCalculationService {

    /**
     * 종합 점수를 기반으로 절대평가 등급 코드를 산출합니다.
     *
     * @param totalScore 종합 점수 (0~100)
     * @return 등급 코드 (S/A/B/C/D)
     */
    String determineGrade(int totalScore);

    /**
     * 특정 평가 차수에서 특정 사원의 종합 점수를 계산합니다.
     * 유형별 가중치(evaluation_type_weights_51)를 적용한 가중 합산 방식입니다.
     *
     * @param periodId 평가 차수 ID
     * @param empId    사원 ID
     * @return 종합 점수 (0~100), 평가 데이터가 없으면 null
     */
    Integer calculateTotalScore(Long periodId, Long empId);

    /**
     * 원천 점수를 0~100점 사이로 정규화합니다. (Min-Max Scaling 등 활용)
     *
     * @param rawScore 원천 점수
     * @param minScore 최소 가능 점수
     * @param maxScore 최대 가능 점수
     * @return 정규화된 점수 (0-100)
     */
    java.math.BigDecimal normalizeScore(java.math.BigDecimal rawScore, java.math.BigDecimal minScore, java.math.BigDecimal maxScore);

    /**
     * 운영 성과 지표(KPI)를 기반으로 최종 평가 점수를 산출합니다.
     *
     * @param compositeScore 지점 성과 종합 점수
     * @return 산출된 평가 점수
     */
    java.math.BigDecimal calculateOperationalScore(java.math.BigDecimal compositeScore);
}
