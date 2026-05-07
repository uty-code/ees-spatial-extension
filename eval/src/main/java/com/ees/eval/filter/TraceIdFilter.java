package com.ees.eval.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 요청의 가장 앞단에서 Trace ID를 발급하고 MDC에 저장하는 필터
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String traceId = UUID.randomUUID().toString();
        
        try {
            // MDC에 traceId 저장 (일반 로그에 [traceId] 형식으로 찍히도록 함)
            MDC.put(TRACE_ID_KEY, traceId);
            
            // AOP 등 후순위 계층에서 사용할 수 있도록 Request 속성에 저장
            request.setAttribute(TRACE_ID_KEY, traceId);
            
            filterChain.doFilter(request, response);
        } finally {
            // [중요] 스레드 풀 누수(ThreadLocal Leak) 방지
            MDC.clear();
        }
    }
}
