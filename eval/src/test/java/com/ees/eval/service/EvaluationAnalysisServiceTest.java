package com.ees.eval.service;

import com.ees.eval.dto.DensityDTO;
import com.ees.eval.dto.EvaluationGapDTO;
import com.ees.eval.dto.ExecutiveReviewContextDTO;
import com.ees.eval.mapper.EvaluationAnalysisMapper;
import com.ees.eval.mapper.EvaluationSnapshotMapper;
import com.ees.eval.mapper.EvaluatorMappingMapper;
import com.ees.eval.service.SpatialAnalysisService;
import com.ees.eval.service.impl.EvaluationAnalysisServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EvaluationAnalysisServiceTest {

    @Mock
    private EvaluationAnalysisMapper evaluationAnalysisMapper;

    @Mock
    private EvaluationSnapshotMapper evaluationSnapshotMapper;

    @Mock
    private EvaluatorMappingMapper evaluatorMappingMapper;

    @Mock
    private SpatialAnalysisService spatialAnalysisService;

    @InjectMocks
    private EvaluationAnalysisServiceImpl evaluationAnalysisService;

    @Test
    @DisplayName("Gap 분석 - 점수 차이가 임계치를 넘으면 WARNING 상태가 된다")
    void calculateGapAnalysis_ShouldFlagWarning() {
        // given
        Long periodId = 1L;
        EvaluationGapDTO mockGap = EvaluationGapDTO.builder()
                .empId(1001L)
                .operationalScore(new BigDecimal("70.0"))
                .managerScore(new BigDecimal("86.0")) // Gap = 16
                .build();
        
        given(evaluationAnalysisMapper.findGapAnalysisByPeriodId(periodId))
                .willReturn(List.of(mockGap));

        // when
        List<EvaluationGapDTO> results = evaluationAnalysisService.calculateGapAnalysis(periodId);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGap()).isEqualByComparingTo(new BigDecimal("16.0"));
        assertThat(results.get(0).getStatus()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("Gap 분석 - 점수 차이가 매우 크면 ANOMALY 상태가 된다")
    void calculateGapAnalysis_ShouldFlagAnomaly() {
        // given
        Long periodId = 1L;
        EvaluationGapDTO mockGap = EvaluationGapDTO.builder()
                .empId(1001L)
                .operationalScore(new BigDecimal("60.0"))
                .managerScore(new BigDecimal("95.0")) // Gap = 35
                .build();
        
        given(evaluationAnalysisMapper.findGapAnalysisByPeriodId(periodId))
                .willReturn(List.of(mockGap));

        // when
        List<EvaluationGapDTO> results = evaluationAnalysisService.calculateGapAnalysis(periodId);

        // then
        assertThat(results.get(0).getStatus()).isEqualTo("ANOMALY");
    }

    @Test
    @DisplayName("Executive Context - 점수 가중치와 추세 데이터가 정상 집계된다")
    void getExecutiveReviewContext_ShouldAggregateTrendsAndWeightedScore() {
        // given
        Long periodId = 1L;
        EvaluationGapDTO mockGap = EvaluationGapDTO.builder()
                .empId(1001L)
                .empName("홍길동")
                .operationalScore(new BigDecimal("80.0"))
                .managerScore(new BigDecimal("90.0"))
                .build();
        
        given(evaluationAnalysisMapper.findGapAnalysisByPeriodId(periodId))
                .willReturn(List.of(mockGap));
        
        java.util.Map<String, Object> trend1 = new java.util.HashMap<>();
        trend1.put("periodName", "2023-Q4");
        trend1.put("score", 75.0);
        
        given(evaluationAnalysisMapper.findTrendDataByEmpId(1001L, 4))
                .willReturn(List.of(trend1));
        
        given(spatialAnalysisService.calculateDensityByEmpId(1001L))
                .willReturn(DensityDTO.builder().densityLevel("HIGH").nearbySameBrandCount(7).build());
        
        given(spatialAnalysisService.getDifficultyCoefficient("HIGH"))
                .willReturn(new BigDecimal("1.05"));

        // when
        List<ExecutiveReviewContextDTO> results = evaluationAnalysisService.getExecutiveReviewContext(periodId, null);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDensityLevel()).isEqualTo("HIGH");
        assertThat(results.get(0).getDifficultyCoefficient()).isEqualByComparingTo(new BigDecimal("1.05"));
        assertThat(results.get(0).isSpatialAdjusted()).isTrue();
    }
}
