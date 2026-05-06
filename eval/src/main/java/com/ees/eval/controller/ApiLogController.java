package com.ees.eval.controller;

import com.ees.eval.domain.ApiLog;
import com.ees.eval.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * API 호출 이력 로그 조회 컨트롤러입니다.
 * 관리자(ROLE_ADMIN)만 접근 가능합니다.
 */
@Slf4j
@Controller
@RequestMapping("/admin/api-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApiLogController {

    private final ApiLogService apiLogService;

    /**
     * API 호출 이력 전체 목록을 조회합니다.
     *
     * @param model 뷰에 전달할 Model 객체
     * @return 뷰 템플릿 경로
     */
    @GetMapping
    public String list(Model model) {
        List<ApiLog> logs = apiLogService.findAll();
        model.addAttribute("logs", logs);
        return "admin/api-logs";
    }
}
