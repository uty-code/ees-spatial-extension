package com.ees.eval.controller;

import com.ees.eval.dto.BranchRiskDTO;
import com.ees.eval.service.BranchRiskService;
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
class BranchRiskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BranchRiskService branchRiskService;

    @InjectMocks
    private BranchRiskController branchRiskController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(branchRiskController).build();
    }

    @Test
    @DisplayName("폐점 위험 지점 분석 조회 - 정상 호출 시 JSON 데이터 반환")
    void getRiskBranches_Success() throws Exception {
        // given
        List<BranchRiskDTO> mockData = List.of(
                BranchRiskDTO.builder()
                        .branchId(101L)
                        .branchName("스타벅스 강남점")
                        .address("서울 강남구")
                        .regionCode("02")
                        .revenueGrowth(new BigDecimal("-15.0"))
                        .claimCount(10)
                        .riskReason("매출 급감 및 클레임 다수")
                        .build()
        );

        given(branchRiskService.getAtRiskBranches()).willReturn(mockData);

        // when & then
        mockMvc.perform(get("/api/branches/risk")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].branchId").value(101))
                .andExpect(jsonPath("$[0].branchName").value("스타벅스 강남점"))
                .andExpect(jsonPath("$[0].revenueGrowth").value(-15.0));
    }
}
