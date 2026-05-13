package com.ees.eval.controller;

import com.ees.eval.domain.Employee;
import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.dto.EvaluatorMappingDTO;
import com.ees.eval.mapper.DepartmentMapper;
import com.ees.eval.mapper.EmployeeMapper;
import com.ees.eval.mapper.EvaluationMapper;
import com.ees.eval.service.EvaluationElementService;
import com.ees.eval.service.EvaluationPeriodService;
import com.ees.eval.service.EvaluationTypeWeightService;
import com.ees.eval.service.EvaluatorMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MyEvaluationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EvaluationPeriodService periodService;

    @Mock
    private EvaluatorMappingService mappingService;

    @Mock
    private EvaluationElementService elementService;

    @Mock
    private EvaluationTypeWeightService typeWeightService;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private MyEvaluationController myEvaluationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(myEvaluationController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        // SecurityContext 설정 (UserDetails.getUsername()이 "1001"을 반환하도록 설정)
        UserDetails userDetails = new User("1001", "password", Collections.emptyList());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("평가 차수가 PLANNED 상태일 때 자가평가 폼 진입 차단 테스트")
    void blockAccessWhenPeriodIsPlanned() throws Exception {
        // Given
        Long mappingId = 1L;
        Long periodId = 10L;

        EvaluatorMappingDTO mapping = EvaluatorMappingDTO.builder()
                .mappingId(mappingId)
                .periodId(periodId)
                .evaluateeId(1001L)
                .relationTypeCode("SELF")
                .build();

        EvaluationPeriodDTO period = EvaluationPeriodDTO.builder()
                .periodId(periodId)
                .statusCode("PLANNED")
                .build();

        given(mappingService.getMappingById(mappingId)).willReturn(mapping);
        given(periodService.getPeriodById(periodId)).willReturn(period);

        // When & Then
        mockMvc.perform(get("/eval/my-evaluation/form")
                        .param("mappingId", mappingId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/eval/my-evaluation?periodId=" + periodId))
                .andExpect(flash().attribute("errorMessage", "평가 시작 전입니다. 평가 기간에 다시 접속해 주세요."));
    }

    @Test
    @DisplayName("평가 차수가 PLANNED 상태일 때 목록 페이지에 안내 메시지 표시 테스트")
    void showInfoMessageWhenPeriodIsPlanned() throws Exception {
        // Given
        Long periodId = 10L;
        Long empId = 1001L;
        EvaluationPeriodDTO period = EvaluationPeriodDTO.builder()
                .periodId(periodId)
                .statusCode("PLANNED")
                .build();

        Employee emp = new Employee();
        emp.setEmpId(empId);
        emp.setDeptId(1L);

        given(periodService.getInProgressPeriods()).willReturn(Collections.singletonList(period));
        given(periodService.getPeriodById(periodId)).willReturn(period);
        given(employeeMapper.findById(empId)).willReturn(Optional.of(emp));
        given(departmentMapper.countDepartmentsByLeaderId(empId)).willReturn(0); // 부서장 아님

        // When & Then
        mockMvc.perform(get("/eval/my-evaluation")
                        .param("periodId", periodId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("eval/my-evaluation/list"))
                .andExpect(model().attribute("infoMessage", "현재 평가 시작 전입니다. 정해진 평가 기간에만 작성이 가능합니다."));
    }
}
