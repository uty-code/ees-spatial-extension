package com.ees.eval.controller;

import com.ees.eval.dto.BranchQuarterlyPerformanceDTO;
import com.ees.eval.dto.ManagerPerformanceDTO;
import com.ees.eval.service.ManagerPerformanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ManagerPerformanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManagerPerformanceService managerPerformanceService;

    @InjectMocks
    private ManagerPerformanceController managerPerformanceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(managerPerformanceController).build();
    }

    @Test
    @DisplayName("관리자 성과 분석 조회 - 정상 호출 시 JSON 데이터 반환")
    void getManagerPerformance_Success() throws Exception {
        // given
        Long empId = 1001L;
        ManagerPerformanceDTO mockData = ManagerPerformanceDTO.builder()
                .empId(empId)
                .avgCompositeScore(new BigDecimal("88.5"))
                .quarterlyTrends(List.of(
                        BranchQuarterlyPerformanceDTO.builder()
                                .perfYear(2023)
                                .perfQuarter(4)
                                .compositeScore(new BigDecimal("87.0"))
                                .build(),
                        BranchQuarterlyPerformanceDTO.builder()
                                .perfYear(2024)
                                .perfQuarter(1)
                                .compositeScore(new BigDecimal("90.0"))
                                .build()
                ))
                .build();

        given(managerPerformanceService.getPerformanceByManager(empId)).willReturn(mockData);

        // when & then
        mockMvc.perform(get("/api/managers/{empId}/performance", empId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empId").value(empId))
                .andExpect(jsonPath("$.avgCompositeScore").value(88.5))
                .andExpect(jsonPath("$.quarterlyTrends[0].perfYear").value(2023))
                .andExpect(jsonPath("$.quarterlyTrends[0].perfQuarter").value(4))
                .andExpect(jsonPath("$.quarterlyTrends[0].compositeScore").value(87.0));
    }
}
