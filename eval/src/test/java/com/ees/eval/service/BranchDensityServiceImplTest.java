package com.ees.eval.service;

import com.ees.eval.dto.BranchDensityDTO;
import com.ees.eval.mapper.BranchDensityMapper;
import com.ees.eval.service.impl.BranchDensityServiceImpl;
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
class BranchDensityServiceImplTest {

    @Mock
    private BranchDensityMapper branchDensityMapper;

    @InjectMocks
    private BranchDensityServiceImpl branchDensityService;

    @Test
    @DisplayName("밀집도 분석 데이터를 정상적으로 계산하고 반환한다")
    void getBranchDensity_ShouldCalculateAndReturnData() {
        // given
        double lat = 37.4979;
        double lng = 127.0276;
        double radius = 1000.0;

        List<BranchDensityDTO.NearbyBranchDTO> mockBranches = List.of(
                BranchDensityDTO.NearbyBranchDTO.builder()
                        .branchId(1L)
                        .revenueGrowth(new BigDecimal("10.0"))
                        .build(),
                BranchDensityDTO.NearbyBranchDTO.builder()
                        .branchId(2L)
                        .revenueGrowth(new BigDecimal("-2.0"))
                        .build()
        );

        given(branchDensityMapper.selectNearbyBranches(lat, lng, radius)).willReturn(mockBranches);

        // when
        BranchDensityDTO result = branchDensityService.getBranchDensity(lat, lng, radius);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getBranchCount()).isEqualTo(2);
        assertThat(result.getAvgRevenueGrowth()).isEqualTo(new BigDecimal("4.00")); // (10 - 2) / 2 = 4
        assertThat(result.getNearbyBranches()).hasSize(2);
    }
}
