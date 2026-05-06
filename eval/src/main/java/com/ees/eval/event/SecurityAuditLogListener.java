package com.ees.eval.event;

import com.ees.eval.domain.ApiLog;
import com.ees.eval.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAuditLogListener {

    private final ApiLogService apiLogService;

    @Async
    @EventListener
    public void handleSecurityAuditLogEvent(SecurityAuditLogEvent event) {
        try {
            Long createdBy = null;
            if (event.getEmpId() != null) {
                try {
                    createdBy = Long.parseLong(event.getEmpId());
                } catch (NumberFormatException ignored) {
                }
            }

            ApiLog apiLog = ApiLog.builder()
                    .apiUrl(event.getUri())
                    .httpMethod(event.getMethod())
                    .requestContent("User-Agent: " + event.getUserAgent())
                    .responseContent("Security exception triggered")
                    .resultCode(event.getResultCode())
                    .ipAddress(event.getIp())
                    .createdBy(createdBy)
                    .build();

            apiLogService.saveLog(apiLog);
        } catch (Exception e) {
            log.error("Failed to save security audit log asynchronously: ", e);
        }
    }
}
