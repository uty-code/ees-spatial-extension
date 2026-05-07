package com.ees.eval.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SecurityAuditLogEvent extends ApplicationEvent {
    
    private final String resultCode; // "UNAUTHORIZED" or "FORBIDDEN"
    private final String method;
    private final String uri;
    private final String ip;
    private final String empId;      // 사번 (401인 경우 null일 수 있음)
    private final String userAgent;
    private final String traceId;

    public SecurityAuditLogEvent(Object source, String resultCode, String method, String uri, String ip, String empId, String userAgent, String traceId) {
        super(source);
        this.resultCode = resultCode;
        this.method = method;
        this.uri = uri;
        this.ip = ip;
        this.empId = empId;
        this.userAgent = userAgent;
        this.traceId = traceId;
    }
}
