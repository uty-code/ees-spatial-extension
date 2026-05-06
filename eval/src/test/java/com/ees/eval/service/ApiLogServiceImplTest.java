package com.ees.eval.service;

import com.ees.eval.domain.ApiLog;
import com.ees.eval.mapper.ApiLogMapper;
import com.ees.eval.service.impl.ApiLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * {@link ApiLogServiceImpl} 단위 테스트입니다.
 * JUnit 5 + Mockito를 사용합니다.
 */
@ExtendWith(MockitoExtension.class)
class ApiLogServiceImplTest {

    @Mock
    private ApiLogMapper apiLogMapper;

    @InjectMocks
    private ApiLogServiceImpl apiLogService;

    @Test
    @DisplayName("should_insert_log_when_valid_apiLog - 정상적인 ApiLog 저장 시 Mapper가 호출되어야 한다")
    void should_insert_log_when_valid_apiLog() {
        // given
        ApiLog apiLog = ApiLog.builder()
                .apiUrl("/eval/performance/submit")
                .httpMethod("POST")
                .requestContent("{\"mappingId\":1}")
                .responseContent("redirect:/eval/performance/form?mappingId=1")
                .resultCode("SUCCESS")
                .ipAddress("127.0.0.1")
                .createdBy(1001L)
                .build();

        // when
        apiLogService.saveLog(apiLog);

        // then
        verify(apiLogMapper, times(1)).insertApiLog(apiLog);
    }

    @Test
    @DisplayName("should_not_throw_when_mapper_실패 - Mapper 예외 발생 시에도 예외가 전파되지 않아야 한다")
    void should_not_throw_when_mapper_fails() {
        // given
        ApiLog apiLog = ApiLog.builder()
                .apiUrl("/eval/performance/submit")
                .httpMethod("POST")
                .resultCode("SUCCESS")
                .build();

        doThrow(new RuntimeException("DB 연결 실패"))
                .when(apiLogMapper).insertApiLog(apiLog);

        // when & then - 예외가 전파되지 않아야 함
        apiLogService.saveLog(apiLog);

        verify(apiLogMapper, times(1)).insertApiLog(apiLog);
    }

    @Test
    @DisplayName("should_insert_log_when_최소_필드만_존재 - 필수 필드만 있어도 저장되어야 한다")
    void should_insert_log_when_minimal_fields() {
        // given
        ApiLog apiLog = ApiLog.builder()
                .apiUrl("/eval/my-evaluation/submit")
                .httpMethod("POST")
                .resultCode("SUCCESS")
                .build();

        // when
        apiLogService.saveLog(apiLog);

        // then
        verify(apiLogMapper, times(1)).insertApiLog(apiLog);
    }
}
