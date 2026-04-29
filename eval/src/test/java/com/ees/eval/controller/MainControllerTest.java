package com.ees.eval.controller;

import com.ees.eval.dto.EmployeeDTO;
import com.ees.eval.service.DepartmentService;
import com.ees.eval.service.EmployeeService;
import com.ees.eval.service.EvaluationPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MainController의 대시보드 성능 최적화 검증 테스트입니다.
 * getAllXxx().size() 대신 count 전용 메서드를 사용하는지 확인합니다.
 */
@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private EvaluationPeriodService evaluationPeriodService;

    @InjectMocks
    private MainController mainController;

    @BeforeEach
    void setUp() {
        // Circular view path 방지용 ViewResolver 설정
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(mainController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("대시보드 - count 전용 메서드를 호출하며 getAllXxx()를 호출하지 않는다")
    void dashboard_ShouldUseCountMethodsInsteadOfGetAll() throws Exception {
        // given: count 메서드 mock 설정
        given(employeeService.countActiveEmployees()).willReturn(50L);
        given(departmentService.countActiveDepartments()).willReturn(8L);
        given(evaluationPeriodService.countByStatusCode("IN_PROGRESS")).willReturn(1L);
        given(evaluationPeriodService.countAll()).willReturn(5L);
        given(employeeService.getTop5RecentEmployees()).willReturn(List.of());

        // when & then: 대시보드 정상 렌더링 확인
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("employeeCount", 50L))
                .andExpect(model().attribute("departmentCount", 8L))
                .andExpect(model().attribute("activePeriodCount", 1L))
                .andExpect(model().attribute("totalPeriodCount", 5L));

        // then: 무거운 getAllXxx() 메서드가 호출되지 않았는지 검증
        verify(employeeService, never()).getAllEmployees();
        verify(departmentService, never()).getAllDepartments();
        verify(evaluationPeriodService, never()).getAllPeriods();

        // count 전용 메서드가 호출되었는지 검증
        verify(employeeService).countActiveEmployees();
        verify(departmentService).countActiveDepartments();
        verify(evaluationPeriodService).countByStatusCode("IN_PROGRESS");
        verify(evaluationPeriodService).countAll();
    }

    @Test
    @DisplayName("대시보드 - 모델에 필수 속성이 모두 포함된다")
    void dashboard_ShouldContainAllRequiredAttributes() throws Exception {
        // given
        given(employeeService.countActiveEmployees()).willReturn(100L);
        given(departmentService.countActiveDepartments()).willReturn(12L);
        given(evaluationPeriodService.countByStatusCode("IN_PROGRESS")).willReturn(0L);
        given(evaluationPeriodService.countAll()).willReturn(3L);
        given(employeeService.getTop5RecentEmployees()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(
                        "employeeCount",
                        "departmentCount",
                        "activePeriodCount",
                        "totalPeriodCount",
                        "gradeStats",
                        "deptStats",
                        "welcomeMessage",
                        "recentEmployees"
                ));
    }
}
