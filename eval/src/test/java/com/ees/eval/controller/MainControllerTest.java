package com.ees.eval.controller;

import com.ees.eval.dto.DashboardStatsDTO;
import com.ees.eval.service.DashboardService;
import com.ees.eval.service.EmployeeService;
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
 * MainController의 대시보드 화면 렌더링 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private MainController mainController;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(mainController)
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .setViewResolvers(viewResolver)
                .build();

        UserDetails userDetails = new User("1001", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("관리자 대시보드 - 정상적으로 DashboardService를 호출하고 모델에 속성을 담는다")
    void dashboard_ForManagement() throws Exception {
        // given
        DashboardStatsDTO stats = new DashboardStatsDTO(
                100L, 10L, "2024 상반기", 90, 10, 85.5,
                java.util.Map.of("S", 10L), java.util.Map.of("영업본부", 80.0), List.of()
        );
        given(dashboardService.getDashboardStats()).willReturn(stats);
        given(employeeService.getTop5RecentEmployees()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("employeeCount", 100L))
                .andExpect(model().attribute("departmentCount", 10L))
                .andExpect(model().attribute("activePeriodName", "2024 상반기"))
                .andExpect(model().attributeExists("gradeStats", "deptStats", "recentActivities", "recentEmployees"));
    }
}
