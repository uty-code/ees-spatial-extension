package com.ees.eval.event;

import com.ees.eval.domain.ApiLog;
import com.ees.eval.service.ApiLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SecurityAuditLogListenerTest {

    @Mock
    private ApiLogService apiLogService;

    @InjectMocks
    private SecurityAuditLogListener securityAuditLogListener;

    @Test
    @DisplayName("401 권한 없음 이벤트 수신 시 ApiLogService를 통해 비동기로 로그를 저장해야 한다")
    void should_save_log_when_401_unauthorized_event_received() {
        // given
        SecurityAuditLogEvent event = new SecurityAuditLogEvent(
                this,
                "UNAUTHORIZED",
                "GET",
                "/api/admin/data",
                "192.168.0.5",
                null, // 401은 사번 없음
                "Mozilla/5.0",
                "test-trace-123"
        );

        // when
        securityAuditLogListener.handleSecurityAuditLogEvent(event);

        // then
        ArgumentCaptor<ApiLog> captor = ArgumentCaptor.forClass(ApiLog.class);
        verify(apiLogService, times(1)).saveLog(captor.capture());

        ApiLog savedLog = captor.getValue();
        assertThat(savedLog.getResultCode()).isEqualTo("UNAUTHORIZED");
        assertThat(savedLog.getCreatedBy()).isNull();
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.0.5");
        assertThat(savedLog.getRequestContent()).contains("Mozilla/5.0");
        assertThat(savedLog.getTraceId()).isEqualTo("test-trace-123");
    }

    @Test
    @DisplayName("403 접근 거부 이벤트 수신 시 사번을 포함하여 로그를 저장해야 한다")
    void should_save_log_with_empid_when_403_forbidden_event_received() {
        // given
        SecurityAuditLogEvent event = new SecurityAuditLogEvent(
                this,
                "FORBIDDEN",
                "POST",
                "/api/executive/data",
                "10.0.0.1",
                "20230001", // 403은 사번 존재
                "Chrome/100",
                "test-trace-456"
        );

        // when
        securityAuditLogListener.handleSecurityAuditLogEvent(event);

        // then
        ArgumentCaptor<ApiLog> captor = ArgumentCaptor.forClass(ApiLog.class);
        verify(apiLogService, times(1)).saveLog(captor.capture());

        ApiLog savedLog = captor.getValue();
        assertThat(savedLog.getResultCode()).isEqualTo("FORBIDDEN");
        assertThat(savedLog.getCreatedBy()).isEqualTo(20230001L);
        assertThat(savedLog.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(savedLog.getRequestContent()).contains("Chrome/100");
    }
}
