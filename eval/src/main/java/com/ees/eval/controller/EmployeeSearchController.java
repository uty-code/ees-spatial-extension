package com.ees.eval.controller;

import com.ees.eval.dto.EmployeeDTO;
import com.ees.eval.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 사원 자동완성 검색용 JSON API 컨트롤러입니다.
 * 프론트엔드 JavaScript에서 fetch()로 호출하여 사원 목록을 JSON으로 수신합니다.
 */
@RestController
@RequestMapping("/api/employees/search")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EmployeeSearchController {

    private final EmployeeService employeeService;

    /**
     * 사원 자동완성 검색 결과를 JSON으로 반환합니다.
     *
     * @param keyword 검색어 (null 허용 - 전체 조회)
     * @return 사원 DTO 리스트 (empId, name, deptName)
     */
    @GetMapping
    public List<EmployeeDTO> search(
            @RequestParam(value = "keyword", required = false) String keyword) {
        // 전체 재직 사원을 50명 제한으로 조회 (프론트에서 JS 필터링)
        return employeeService.searchForAutocomplete(keyword, 50);
    }
}
