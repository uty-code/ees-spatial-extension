package com.ees.eval.security;

import com.ees.eval.event.SecurityAuditLogEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();

        // AlwaysOn(상태 체크 봇) 및 파비콘 요청은 감사 로그 기록에서 제외
        if ((userAgent != null && userAgent.contains("AlwaysOn")) || uri.endsWith(".ico")) {
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            } else {
                request.getRequestDispatcher("/error-page/403").forward(request, response);
            }
            return;
        }

        String empId = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            empId = authentication.getName(); // Spring Security UserDetails의 username(사번)
        }
        
        String traceId = (String) request.getAttribute("traceId");

        // 403 Forbidden 에러
        SecurityAuditLogEvent event = new SecurityAuditLogEvent(
                this,
                "FORBIDDEN",
                request.getMethod(),
                uri,
                ip,
                empId,
                userAgent,
                traceId
        );
        eventPublisher.publishEvent(event);

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        } else {
            // 리다이렉트 대신 포워드를 사용하여 URL을 유지하고 Trace ID를 공유합니다.
            request.getRequestDispatcher("/error-page/403").forward(request, response);
        }
    }
}
