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
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link ApiLoggable} 어노테이션이 부착된 컨트롤러 메소드 혹은
 * 에러가 발생한 모든 컨트롤러 메소드의 API 호출 이력을 자동으로 기록하는 AOP Aspect입니다.
 *
 * <p>실행 흐름:</p>
 * <ol>
 *   <li>컨트롤러 호출 감지</li>
 *   <li>{@code HttpServletRequest}에서 URL, HTTP Method, IP 추출</li>
 *   <li>메소드 파라미터를 JSON으로 직렬화하여 요청 내용 기록</li>
 *   <li>메소드 실행 후, @ApiLoggable이 있거나 에러가 발생한 경우 결과(혹은 에러 메시지)를 응답 내용으로 기록</li>
 *   <li>{@link ApiLogService}를 통해 {@code api_logs_51} 테이블에 저장</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private final ApiLogService apiLogService;
    private final ObjectMapper objectMapper;

    /**
     * 컨트롤러 메소드 실행 전후로 API 호출 이력을 기록합니다.
     * @ApiLoggable이 선언된 메소드거나 예외가 발생한 메소드의 경우 이력을 기록합니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @return 원본 메소드의 반환값
     * @throws Throwable 원본 메소드에서 발생한 예외
     */
    @Around("@annotation(com.ees.eval.aop.ApiLoggable) || execution(* com.ees.eval.controller..*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // @ApiLoggable 어노테이션 여부 미리 확인 (불필요한 연산 방지용)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        boolean isLoggable = signature.getMethod().isAnnotationPresent(ApiLoggable.class);

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
            // @ApiLoggable이 붙어 있거나, 에러가 발생한 경우에만 실제 데이터 추출 및 로깅 (성능 최적화)
            if (isLoggable || hasError) {
                // 1. HttpServletRequest에서 URL, Method, IP 추출
                HttpServletRequest request = getCurrentRequest();
                String apiUrl = (request != null) ? request.getRequestURI() : "UNKNOWN";
                String httpMethod = (request != null) ? request.getMethod() : "UNKNOWN";
                String ipAddress = extractClientIp(request);
                Long empId = extractEmployeeId(joinPoint);

                // 2. 파라미터 직렬화 (필요한 순간에만 수행하여 성능 저하 방지)
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
                        .createdBy(empId)
                        .build();

                apiLogService.saveLog(apiLog);
            }
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
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
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
        if (request == null) return "UNKNOWN";

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
     * 메소드 파라미터에서 사원 ID를 추출합니다.
     * {@code @AuthenticationPrincipal UserDetails}의 username을 사원 ID로 사용합니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @return 사원 ID 또는 null
     */
    private Long extractEmployeeId(ProceedingJoinPoint joinPoint) {
        try {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                    return Long.parseLong(userDetails.getUsername());
                }
            }
        } catch (Exception e) {
            log.debug("[API 로그] 사원 ID 추출 실패: {}", e.getMessage());
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
            if (request == null) return "{}";

            // HttpServletRequest에서 전체 파라미터 맵을 직접 읽기
            Map<String, String[]> parameterMap = request.getParameterMap();
            // 단일 값 파라미터는 배열 대신 문자열로 변환하여 가독성 향상
            Map<String, Object> cleanMap = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
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
