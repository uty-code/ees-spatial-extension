package com.ees.eval.service;

import com.ees.eval.dto.BranchRiskDTO;
import com.ees.eval.mapper.BranchRiskMapper;
import com.ees.eval.service.impl.BranchRiskServiceImpl;
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
class BranchRiskServiceImplTest {

    @Mock
    private BranchRiskMapper branchRiskMapper;

    @InjectMocks
    private BranchRiskServiceImpl branchRiskService;

    @Test
    @DisplayName("폐점 위험 지점 목록을 정상적으로 반환한다")
    void getAtRiskBranches_ShouldReturnMappedData() {
        // given
        List<BranchRiskDTO> expectedData = List.of(
                BranchRiskDTO.builder()
                        .branchId(101L)
                        .branchName("스타벅스 강남점")
                        .revenueGrowth(new BigDecimal("-15.0"))
                        .claimCount(10)
                        .build()
        );
        given(branchRiskMapper.selectAtRiskBranches()).willReturn(expectedData);

        // when
        List<BranchRiskDTO> actualData = branchRiskService.getAtRiskBranches();

        // then
        assertThat(actualData).isNotNull();
        assertThat(actualData).hasSize(1);
        assertThat(actualData.get(0).getBranchId()).isEqualTo(101L);
    }
}
