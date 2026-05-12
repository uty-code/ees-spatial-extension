package com.ees.eval.controller;

import com.ees.eval.dto.BranchDensityDTO;
import com.ees.eval.service.BranchDensityService;
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

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BranchDensityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BranchDensityService branchDensityService;

    @InjectMocks
    private BranchDensityController branchDensityController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(branchDensityController).build();
    }

    @Test
    @DisplayName("밀집도 분석 조회 - 정상 호출 시 JSON 데이터 반환")
    void getBranchDensity_Success() throws Exception {
        // given
        BranchDensityDTO mockData = BranchDensityDTO.builder()
                .branchCount(2)
                .avgRevenueGrowth(new BigDecimal("5.5"))
                .nearbyBranches(List.of(
                        BranchDensityDTO.NearbyBranchDTO.builder()
                                .branchId(1L)
                                .branchName("강남역점")
                                .distanceMeters(500.0)
                                .revenueGrowth(new BigDecimal("10.0"))
                                .build(),
                        BranchDensityDTO.NearbyBranchDTO.builder()
                                .branchId(2L)
                                .branchName("신논현점")
                                .distanceMeters(800.0)
                                .revenueGrowth(new BigDecimal("1.0"))
                                .build()
                ))
                .build();

        given(branchDensityService.getBranchDensity(37.4979, 127.0276, 1000.0)).willReturn(mockData);

        // when & then
        mockMvc.perform(get("/api/branches/density")
                .param("lat", "37.4979")
                .param("lng", "127.0276")
                .param("radius", "1000.0")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchCount").value(2))
                .andExpect(jsonPath("$.avgRevenueGrowth").value(5.5))
                .andExpect(jsonPath("$.nearbyBranches[0].branchName").value("강남역점"))
                .andExpect(jsonPath("$.nearbyBranches[0].distanceMeters").value(500.0));
    }
}
