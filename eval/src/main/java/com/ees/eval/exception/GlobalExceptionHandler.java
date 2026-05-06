package com.ees.eval.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger clientErrorLogger = LoggerFactory.getLogger("CLIENT_ERROR");

    /**
     * 클라이언트 에러를 단일 라인으로 로깅하는 공통 메서드
     */
    private void logClientError(HttpServletRequest request, String errorMessage) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        
        clientErrorLogger.warn("[{} {}] IP: {} - {}", method, uri, ip, errorMessage);
    }

    @ExceptionHandler(EesOptimisticLockException.class)
    public String handleOptimisticLockException(EesOptimisticLockException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logClientError(request, "낙관적 락 충돌: " + ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", "다른 관리자가 동일한 데이터를 수정 중입니다. 페이지를 새로고침하고 다시 시도해 주세요.");
        return "redirect:/eval/periods";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logClientError(request, "비즈니스 규칙 위반: " + ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/eval/periods";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request, Model model) {
        String firstErrorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        logClientError(request, "유효성 검사 실패: " + firstErrorMessage);
        model.addAttribute("errorMessage", firstErrorMessage);
        model.addAttribute("statusCode", HttpStatus.BAD_REQUEST.value());
        return "error/custom-error";
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public String handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request, Model model) {
        logClientError(request, "잘못된 요청 형식 (JSON 파싱/타입 오류): " + ex.getMessage());
        model.addAttribute("errorMessage", "잘못된 요청 형식입니다.");
        model.addAttribute("statusCode", HttpStatus.BAD_REQUEST.value());
        return "error/custom-error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request, Model model) {
        logClientError(request, "잘못된 요청 파라미터: " + ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("statusCode", HttpStatus.BAD_REQUEST.value());
        return "error/custom-error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request, Model model) {
        String uri = request.getRequestURI();
        // 파비콘 및 기타 무의미한 정적 리소스 요청은 로깅 무시
        if (!uri.endsWith(".ico") && !uri.endsWith(".map")) {
            logClientError(request, "존재하지 않는 API 주소 요청: " + uri);
        }
        model.addAttribute("errorMessage", "요청하신 페이지를 찾을 수 없습니다.");
        model.addAttribute("statusCode", HttpStatus.NOT_FOUND.value());
        return "error/custom-error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("예기치 않은 서버 오류 발생: ", ex);
        model.addAttribute("errorMessage", "시스템에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        model.addAttribute("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return "error/custom-error";
    }
}
