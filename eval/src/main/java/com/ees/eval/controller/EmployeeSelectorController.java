package com.ees.eval.controller;

import com.ees.eval.dto.DepartmentDTO;
import com.ees.eval.dto.EmployeePageDTO;
import com.ees.eval.service.DepartmentService;
import com.ees.eval.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 사원 선택기(Employee Selector)를 위한 HTMX 조각을 제공하는 컨트롤러입니다.
 * 모달 내에서 부서별 필터링, 검색, 페이지네이션을 처리합니다.
 */
@Slf4j
@Controller
@RequestMapping("/employees/selector")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EXECUTIVE')")
public class EmployeeSelectorController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    /**
     * 사원 선택기 모달의 전체 조각을 반환합니다. (최초 로드 시)
     */
    @GetMapping
    public String getSelector(Model model) {
        List<DepartmentDTO> departments = departmentService.getAllDepartments();
        model.addAttribute("selectorDepartments", departments);
        
        // 초기 데이터 (전체 부서, 1페이지)
        EmployeePageDTO page = employeeService.searchEmployeesPage(null, null, "EMPLOYED", 1, 100);
        model.addAttribute("selectorPage", page);
        
        return "fragments/employee-selector :: selector-modal";
    }

    /**
     * 필터링된 사원 목록 조각만 반환합니다. (HTMX 전용)
     */
    @GetMapping("/list")
    public String getSelectorList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            Model model) {
        
        // 검색 시에는 페이지 사이즈를 넉넉하게 (무한 스크롤 대신 일단 넉넉히)
        EmployeePageDTO page = employeeService.searchEmployeesPage(keyword, deptId, "EMPLOYED", 1, 100);
        model.addAttribute("selectorPage", page);
        
        return "fragments/employee-selector :: employee-list";
    }
}
