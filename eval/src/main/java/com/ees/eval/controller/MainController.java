package com.ees.eval.controller;

import com.ees.eval.dto.DashboardStatsDTO;
import com.ees.eval.dto.EmployeeDashboardDTO;
import com.ees.eval.service.DashboardService;
import com.ees.eval.service.DepartmentService;
import com.ees.eval.service.EmployeeService;
import com.ees.eval.service.EvaluationPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * 메인 대시보드 및 공통 화면 접근을 담당하는 컨트롤러입니다.
 * 시스템 현황 통계 정보를 화면에 제공합니다.
 */
@Controller
@RequiredArgsConstructor
public class MainController {

    private final EmployeeService employeeService;
    private final DashboardService dashboardService;

    /**
     * 메인 대시보드 화면을 반환합니다.
     * 평가 완료율, 등급 분포, 부서별 점수 등 실시간 통계를 제공합니다.
     */
    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Model model, @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        boolean isManagement = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") 
                            || a.getAuthority().equals("ROLE_EXECUTIVE") 
                            || a.getAuthority().equals("ROLE_MANAGER"));

        if (isManagement) {
            DashboardStatsDTO stats = dashboardService.getDashboardStats();
            
            model.addAttribute("stats", stats);
            model.addAttribute("employeeCount", stats.employeeCount());
            model.addAttribute("departmentCount", stats.departmentCount());
            model.addAttribute("activePeriodName", stats.activePeriodName());
            model.addAttribute("completionRate", Math.round(stats.completionRate()));
            
            model.addAttribute("gradeStats", stats.gradeDistribution());
            model.addAttribute("deptStats", stats.deptAverageScores());
            model.addAttribute("recentActivities", stats.recentActivities());

            model.addAttribute("welcomeMessage", "사원 평가 시스템(EES) 관리자 페이지에 오신 것을 환영합니다.");
            model.addAttribute("recentEmployees", employeeService.getTop5RecentEmployees());

            return "dashboard";
        } else {
            // 일반 사원용 대시보드
            Long empId = Long.parseLong(userDetails.getUsername());
            EmployeeDashboardDTO empStats = dashboardService.getEmployeeDashboardStats(empId);
            
            model.addAttribute("empStats", empStats);
            model.addAttribute("welcomeMessage", userDetails.getUsername() + "님, 오늘도 좋은 하루 되세요!");
            
            return "dashboard_emp";
        }
    }
}
