package com.ees.eval.service;

import com.ees.eval.dto.BranchQuarterlyPerformanceDTO;
import com.ees.eval.dto.ManagerPerformanceDTO;
import com.ees.eval.mapper.ManagerPerformanceMapper;
import com.ees.eval.service.impl.ManagerPerformanceServiceImpl;
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
class ManagerPerformanceServiceImplTest {

    @Mock
    private ManagerPerformanceMapper managerPerformanceMapper;

    @InjectMocks
    private ManagerPerformanceServiceImpl managerPerformanceService;

    @Test
    @DisplayName("관리자 성과 분석 조회 - 매퍼 결과를 정상적으로 반환한다")
    void getPerformanceByManager_ShouldReturnMappedData() {
        // given
        Long empId = 1001L;
        ManagerPerformanceDTO expectedData = ManagerPerformanceDTO.builder()
                .empId(empId)
                .avgCompositeScore(new BigDecimal("92.0"))
                .quarterlyTrends(List.of(
                        BranchQuarterlyPerformanceDTO.builder()
                                .perfYear(2023)
                                .perfQuarter(4)
                                .compositeScore(new BigDecimal("91.0"))
                                .build()
                ))
                .build();
        given(managerPerformanceMapper.selectPerformanceByManager(empId)).willReturn(expectedData);

        // when
        ManagerPerformanceDTO actualData = managerPerformanceService.getPerformanceByManager(empId);

        // then
        assertThat(actualData).isNotNull();
        assertThat(actualData.getEmpId()).isEqualTo(empId);
        assertThat(actualData.getAvgCompositeScore()).isEqualTo(new BigDecimal("92.0"));
        assertThat(actualData.getQuarterlyTrends()).hasSize(1);
    }
}
