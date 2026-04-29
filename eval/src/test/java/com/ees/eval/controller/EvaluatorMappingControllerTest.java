package com.ees.eval.controller;

import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.dto.MappingAnomalyDTO;
import com.ees.eval.service.EvaluationPeriodService;
import com.ees.eval.service.EvaluatorMappingService;
import com.ees.eval.mapper.EmployeeMapper;
import com.ees.eval.mapper.DepartmentMapper;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EvaluatorMappingController의 테스트 클래스입니다.
 * 정합성 검증 API와 캐시 제어 헤더 설정을 중점적으로 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class EvaluatorMappingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EvaluatorMappingService mappingService;

    @Mock
    private EvaluationPeriodService periodService;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private EvaluatorMappingController mappingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mappingController).build();
    }

    @Test
    @DisplayName("정합성 검증 API - 캐시 방지 헤더가 포함되어야 한다")
    void validateMappings_ShouldHaveCacheControlHeaders() throws Exception {
        // given
        Long periodId = 1L;
        given(periodService.getPeriodById(periodId)).willReturn(
                EvaluationPeriodDTO.builder().periodId(periodId).periodName("2024 상반기").build()
        );
        given(mappingService.checkMappingIntegrity(periodId)).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/eval/evaluators/validate")
                        .param("periodId", periodId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    @Test
    @DisplayName("정합성 검증 API - 이상 데이터가 있는 경우 JSON 목록을 반환한다")
    void validateMappings_WithAnomalies_ShouldReturnJsonList() throws Exception {
        // given
        Long periodId = 1L;
        List<MappingAnomalyDTO> anomalies = List.of(
                new MappingAnomalyDTO(1001L, "홍길동", "개발팀", "SELF_EVAL", "본인 평가자 미지정", "ERROR")
        );
        given(periodService.getPeriodById(periodId)).willReturn(
                EvaluationPeriodDTO.builder().periodId(periodId).build()
        );
        given(mappingService.checkMappingIntegrity(periodId)).willReturn(anomalies);

        // when & then
        mockMvc.perform(get("/eval/evaluators/validate")
                        .param("periodId", periodId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].severity").value("ERROR"))
                .andExpect(jsonPath("$[0].evaluateeName").value("홍길동"))
                .andExpect(jsonPath("$[0].anomalyType").value("SELF_EVAL"));
    }

    @Test
    @DisplayName("정합성 검증 API - 존재하지 않는 차수 ID 요청 시 400 에러를 반환한다")
    void validateMappings_InvalidPeriod_ShouldReturnBadRequest() throws Exception {
        // given
        given(periodService.getPeriodById(anyLong())).willReturn(null);

        // when & then
        mockMvc.perform(get("/eval/evaluators/validate")
                        .param("periodId", "999"))
                .andExpect(status().isBadRequest());
    }
}
