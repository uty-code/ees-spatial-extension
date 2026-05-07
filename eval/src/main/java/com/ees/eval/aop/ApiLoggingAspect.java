package com.ees.eval.aop;

import com.ees.eval.domain.ApiLog;
import com.ees.eval.service.ApiLogService;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>우선순위를 최상위(Ordered.HIGHEST_PRECEDENCE)로 설정하여, Spring Security의 권한 체크(@PreAuthorize)
 * 에 의해 거부되는 상황에서도 '어떤 메소드 실행을 시도했는지'를 기록할 수 있도록 설계되었습니다.</p>
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private final ApiLogService apiLogService;
    private final ObjectMapper objectMapper;

    /**
     * 컨트롤러 메소드 실행 전후로 API 호출 이력을 기록합니다.
     * 모든 컨트롤러 호출에 대해 이력을 기록합니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @return 원본 메소드의 반환값
     * @throws Throwable 원본 메소드에서 발생한 예외
     */
    @Around("execution(* com.ees.eval.controller..*Controller.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String resultCode = "SUCCESS";
        Object result = null;
        String errorMessage = null;
        boolean hasError = false;

        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            resultCode = "ERROR";
            errorMessage = t.getMessage();
            hasError = true;
            throw t;
        } finally {
            // 모든 API 요청을 항상 기록 (조건부 로깅 해제)

            // 1. HttpServletRequest에서 URL, Method, IP, Trace ID 추출
            HttpServletRequest request = getCurrentRequest();
            String apiUrl = (request != null) ? request.getRequestURI() : "UNKNOWN";
            String httpMethod = (request != null) ? request.getMethod() : "UNKNOWN";
            String ipAddress = extractClientIp(request);
            String traceId = (request != null) ? (String) request.getAttribute("traceId") : null;
            Long empId = extractEmployeeId(joinPoint);
            Long targetId = extractTargetId(joinPoint);

            // 2. 파라미터 직렬화
            String requestContent = serializeParameters(request);

            // 3. 로그 저장 (성공/실패 모두 기록)
            String responseContent = (result != null) ? result.toString() : null;

            // 에러 발생 시 예외 메시지를 응답 내용으로 기록
            if (hasError) {
                responseContent = "ERROR: " + (errorMessage != null ? errorMessage : "Unknown Error");
            }

            // 응답 내용이 너무 길면 잘라내기
            if (responseContent != null && responseContent.length() > 500) {
                responseContent = responseContent.substring(0, 500);
            }

            ApiLog apiLog = ApiLog.builder()
                    .apiUrl(apiUrl)
                    .httpMethod(httpMethod)
                    .requestContent(requestContent)
                    .responseContent(responseContent)
                    .resultCode(resultCode)
                    .ipAddress(ipAddress)
                    .targetId(targetId)
                    .traceId(traceId)
                    .createdBy(empId)
                    .build();

            apiLogService.saveLog(apiLog);
        }

        return result;
    }

    /**
     * 현재 HTTP 요청 객체를 가져옵니다.
     *
     * @return HttpServletRequest 또는 null
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return (attrs != null) ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 클라이언트 IP를 추출합니다. 프록시 환경을 고려하여 X-Forwarded-For 헤더를 우선 확인합니다.
     *
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소
     */
    private String extractClientIp(HttpServletRequest request) {
        if (request == null)
            return "UNKNOWN";

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For에 여러 IP가 있을 경우 첫 번째(원본 클라이언트)만 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 현재 로그인한 사용자의 사원 ID를 추출합니다.
     * 1. 메소드 파라미터에서 UserDetails 객체를 찾습니다.
     * 2. 파라미터에 없을 경우 SecurityContextHolder에서 인증 정보를 가져옵니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @return 사원 ID 또는 null
     */
    private Long extractEmployeeId(ProceedingJoinPoint joinPoint) {
        try {
            // 1. 메소드 파라미터에서 먼저 확인
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                    return Long.parseLong(userDetails.getUsername());
                }
            }

            // 2. 파라미터에 없을 경우 SecurityContext에서 확인
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                return Long.parseLong(userDetails.getUsername());
            }
        } catch (Exception e) {
            log.debug("[API 로그] 사원 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 메소드 파라미터 중 @LogTarget 어노테이션이 붙은 값을 추출합니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @return 추출된 대상 ID 또는 null
     */
    private Long extractTargetId(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            java.lang.annotation.Annotation[][] annotations = signature.getMethod().getParameterAnnotations();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < annotations.length; i++) {
                for (java.lang.annotation.Annotation annotation : annotations[i]) {
                    if (annotation instanceof LogTarget) {
                        if (args[i] instanceof Number number) {
                            return number.longValue();
                        } else if (args[i] instanceof String str) {
                            return Long.parseLong(str);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[API 로그] Target ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * HTTP 요청의 전체 파라미터를 JSON 문자열로 직렬화합니다.
     * Spring MVC의 @RequestParam 바인딩과 무관하게 원본 파라미터를 모두 캡처합니다.
     *
     * @param request HTTP 요청 객체
     * @return JSON 형태의 파라미터 문자열
     */
    private String serializeParameters(HttpServletRequest request) {
        try {
            if (request == null)
                return "{}";

            // HttpServletRequest에서 전체 파라미터 맵을 직접 읽기
            Map<String, String[]> parameterMap = request.getParameterMap();
            // 단일 값 파라미터는 배열 대신 문자열로 변환하여 가독성 향상
            Map<String, Object> cleanMap = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                // 노이즈 파라미터 (캐시 방지용 t) 필터링
                if ("t".equals(entry.getKey())) continue;
                
                String[] values = entry.getValue();
                cleanMap.put(entry.getKey(), (values.length == 1) ? values[0] : values);
            }

            String json = objectMapper.writeValueAsString(cleanMap);
            // 요청 내용이 너무 길면 잘라내기
            if (json.length() > 4000) {
                json = json.substring(0, 4000);
            }
            return json;
        } catch (Exception e) {
            log.debug("[API 로그] 파라미터 직렬화 실패: {}", e.getMessage());
            return "{}";
        }
    }
}
