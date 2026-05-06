package com.ees.eval.security;

import com.ees.eval.event.SecurityAuditLogEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        String userAgent = request.getHeader("User-Agent");

        // 401 Unauthorized 에러: 인증되지 않은 사용자이므로 empId는 null로 처리
        SecurityAuditLogEvent event = new SecurityAuditLogEvent(
                this,
                "UNAUTHORIZED",
                request.getMethod(),
                request.getRequestURI(),
                ip,
                null,
                userAgent
        );
        eventPublisher.publishEvent(event);

        // 기본 /login 페이지로 리다이렉트하거나 401 응답 전송
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        } else {
            response.sendRedirect("/login");
        }
    }
}
