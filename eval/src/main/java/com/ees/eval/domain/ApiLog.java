package com.ees.eval.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 호출 이력(History) 로그를 나타내는 도메인 클래스입니다.
 * 비즈니스 API 호출의 URL, 파라미터, 결과 등을 기록합니다.
 *
 * <p>
 * INSERT-only(삽입 전용) 특성상 {@link BaseEntity}를 상속하지 않고,
 * {@link LoginLog}와 동일한 {@code @Builder} 패턴으로 독립 구현합니다.
 * </p>
 */
@Getter
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ApiLog {

    /** 로그 고유 ID (auto-increment) */
    private Long logId;

    /** 호출된 API URL (예: /eval/performance/submit) */
    private String apiUrl;

    /** HTTP 메소드 (GET, POST 등) */
    private String httpMethod;

    /** 요청 파라미터 (JSON 형태) */
    private String requestContent;

    /** 응답/결과 요약 (JSON 형태) */
    private String responseContent;

    /** 결과 코드 (HTTP 상태 코드 또는 비즈니스 결과 코드) */
    private String resultCode;

    /** 접속 IP (IPv4/IPv6) */
    private String ipAddress;

    /** 비즈니스 주요 식별자 (예: mappingId, empId) */
    private Long targetId;

    /** 단일 요청 전체 흐름을 묶는 고유 ID */
    private String traceId;

    /** 로그 생성 시각 */
    private LocalDateTime createdAt;

    /** 로그 생성자 (호출한 사원 ID) */
    private Long createdBy;
}
