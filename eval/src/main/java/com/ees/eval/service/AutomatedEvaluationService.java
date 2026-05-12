package com.ees.eval.service;

import java.math.BigDecimal;

/**
 * 시스템 자동 평가 및 운영 지표 기반 점수 산출을 담당하는 서비스 인터페이스입니다.
 */
public interface AutomatedEvaluationService {

    /**
     * 특정 사원이 관리하는 지점들의 KPI를 집계하여 정규화된 점수를 산출합니다.
     *
     * @param empId   사원 식별자
     * @param year    평가 연도
     * @param quarter 평가 분기
     * @return 0~100 범위로 정규화된 종합 점수
     */
    BigDecimal calculateBranchKPIs(Long empId, Integer year, Integer quarter);

    /**
     * 특정 사원의 분기 실적을 기반으로 'OPERATIONAL' 타입의 평가 데이터를 자동으로 생성합니다.
     *
     * @param year        평가 연도
     * @param quarter     평가 분기
     * @param evaluateeId 피평가자(사원) 식별자
     */
    void populateSystemEvaluation(Integer year, Integer quarter, Long evaluateeId);
}
