package com.ees.eval.controller;

import com.ees.eval.dto.RegionPerformanceDTO;
import com.ees.eval.service.BranchPerformanceService;
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
class BranchPerformanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BranchPerformanceService branchPerformanceService;

    @InjectMocks
    private BranchPerformanceController branchPerformanceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(branchPerformanceController).build();
    }

    @Test
    @DisplayName("지역별 성과 분석 조회 - 정상 호출 시 JSON 데이터 반환")
    void getPerformanceByRegion_Success() throws Exception {
        // given
        List<RegionPerformanceDTO> mockData = List.of(
                RegionPerformanceDTO.builder()
                        .regionCode("02")
                        .regionName("서울")
                        .avgCompositeScore(new BigDecimal("85.5"))
                        .avgClaimCount(new BigDecimal("2.1"))
                        .build(),
                RegionPerformanceDTO.builder()
                        .regionCode("031")
                        .regionName("경기")
                        .avgCompositeScore(new BigDecimal("82.0"))
                        .avgClaimCount(new BigDecimal("3.5"))
                        .build()
        );

        given(branchPerformanceService.getPerformanceByRegion()).willReturn(mockData);

        // when & then
        mockMvc.perform(get("/api/branches/performance/by-region")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].regionCode").value("02"))
                .andExpect(jsonPath("$[0].regionName").value("서울"))
                .andExpect(jsonPath("$[0].avgCompositeScore").value(85.5))
                .andExpect(jsonPath("$[0].avgClaimCount").value(2.1))
                .andExpect(jsonPath("$[1].regionCode").value("031"))
                .andExpect(jsonPath("$[1].regionName").value("경기"));
    }
}
