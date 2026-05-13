package com.ees.eval.service;

import com.ees.eval.dto.DensityDTO;
import java.math.BigDecimal;

public interface SpatialAnalysisService {
    /**
     * 특정 지점의 동일 브랜드 밀집도를 계산합니다.
     */
    DensityDTO calculateDensity(Long branchId);

    /**
     * 사원이 담당하는 지점의 밀집도를 계산합니다.
     */
    DensityDTO calculateDensityByEmpId(Long empId);

    /**
     * 밀집도 수준에 따른 난이도 보정 계수를 반환합니다.
     */
    BigDecimal getDifficultyCoefficient(String densityLevel);

    /**
     * 보정 계수를 적용하되, 등급별 Bonus Cap을 준수하여 최종 점수를 산출합니다.
     */
    BigDecimal calculateCappedScore(BigDecimal baseScore, BigDecimal coefficient, String densityLevel);

    /**
     * 히트맵 시각화용 데이터를 조회합니다.
     */
    java.util.List<com.ees.eval.dto.HeatMapDTO> getHeatMapData(Long periodId);
}
