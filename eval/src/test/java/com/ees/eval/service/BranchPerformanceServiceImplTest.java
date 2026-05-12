package com.ees.eval.service;

import com.ees.eval.dto.RegionPerformanceDTO;
import com.ees.eval.mapper.BranchPerformanceMapper;
import com.ees.eval.service.impl.BranchPerformanceServiceImpl;
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
class BranchPerformanceServiceImplTest {

    @Mock
    private BranchPerformanceMapper branchPerformanceMapper;

    @InjectMocks
    private BranchPerformanceServiceImpl branchPerformanceService;

    @Test
    @DisplayName("지역별 성과 분석 조회 - 매퍼 결과를 정상적으로 반환한다")
    void getPerformanceByRegion_ShouldReturnMappedData() {
        // given
        List<RegionPerformanceDTO> expectedData = List.of(
                RegionPerformanceDTO.builder()
                        .regionCode("02")
                        .regionName("서울")
                        .avgCompositeScore(new BigDecimal("85.5"))
                        .build()
        );
        given(branchPerformanceMapper.selectPerformanceByRegion()).willReturn(expectedData);

        // when
        List<RegionPerformanceDTO> actualData = branchPerformanceService.getPerformanceByRegion();

        // then
        assertThat(actualData).hasSize(1);
        assertThat(actualData.get(0).getRegionCode()).isEqualTo("02");
        assertThat(actualData.get(0).getRegionName()).isEqualTo("서울");
        assertThat(actualData.get(0).getAvgCompositeScore()).isEqualTo(new BigDecimal("85.5"));
    }
}
