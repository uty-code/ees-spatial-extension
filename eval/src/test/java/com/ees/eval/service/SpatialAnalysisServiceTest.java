package com.ees.eval.service;

import com.ees.eval.dto.HeatMapDTO;
import com.ees.eval.mapper.SpatialAnalysisMapper;
import com.ees.eval.service.impl.SpatialAnalysisServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SpatialAnalysisServiceTest {

    @Mock
    private SpatialAnalysisMapper spatialAnalysisMapper;

    @Mock
    private EvaluationAnalysisService evaluationAnalysisService;

    @InjectMocks
    private SpatialAnalysisServiceImpl spatialAnalysisService;

    @Test
    @DisplayName("HeatMap 데이터 조회 - 지점 좌표와 점수, 리스크 레벨이 정상 병합된다")
    void getHeatMapData_ShouldMergeSpatialAndRiskData() {
        // given
        Long periodId = 1L;
        HeatMapDTO mockDto = HeatMapDTO.builder()
                .branchId(101L)
                .branchName("강남역점")
                .lat(37.4979)
                .lng(127.0276)
                .score(new java.math.BigDecimal("84.0"))
                .empId(1001L)
                .build();

        given(spatialAnalysisMapper.findAllBranchSpatialData(periodId))
                .willReturn(List.of(mockDto));
        
        given(evaluationAnalysisService.calculateGapAnalysis(periodId))
                .willReturn(List.of()); // Empty gap list for simplicity

        given(spatialAnalysisMapper.countNearbySameBrandBranches(anyLong(), anyDouble()))
                .willReturn(7); // HIGH density

        // when
        List<HeatMapDTO> results = spatialAnalysisService.getHeatMapData(periodId);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBranchName()).isEqualTo("강남역점");
        assertThat(results.get(0).getDensityLevel()).isEqualTo("HIGH");
        assertThat(results.get(0).getRiskLevel()).isEqualTo("NORMAL");
    }
}
