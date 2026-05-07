package com.ees.eval.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * 상태 코드별 에러 페이지 처리를 담당하는 컨트롤러입니다.
 * 스프링 부트 기본 에러 컨트롤러와의 충돌을 피하기 위해 /error-page 경로를 사용합니다.
 */
@Controller
public class ErrorPageController {

    private static final Map<Integer, String> ERROR_MESSAGES = Map.of(
        400, "잘못된 요청입니다. 입력값을 확인해 주세요.",
        401, "인증이 필요한 서비스입니다.",
        403, "해당 페이지에 접근할 권한이 없습니다.",
        404, "요청하신 페이지를 찾을 수 없습니다.",
        500, "시스템에 일시적인 오류가 발생했습니다."
    );

    /**
     * 내부 포워딩 등을 통한 에러 처리를 담당합니다.
     */
    @RequestMapping("/error-page")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = (status != null) ? Integer.parseInt(status.toString()) : 500;
        
        return renderErrorPage(statusCode, model);
    }

    /**
     * 명시적인 에러 코드 경로를 처리합니다.
     */
    @GetMapping("/error-page/{code}")
    public String handleErrorWithCode(@PathVariable("code") int code, Model model) {
        return renderErrorPage(code, model);
    }

    private String renderErrorPage(int code, Model model) {
        model.addAttribute("statusCode", code);
        model.addAttribute("errorMessage", ERROR_MESSAGES.getOrDefault(code, "알 수 없는 오류가 발생했습니다."));
        return "error/custom-error";
    }
}
