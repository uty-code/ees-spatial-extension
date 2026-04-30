package com.ees.eval.dto;

import lombok.Builder;

/**
 * 면담 관리 목록에서 보여줄 피평가자 정보와 면담 상태를 담은 DTO입니다.
 */
@Builder
public record InterviewTaskDTO(
    Long mappingId,
    String evaluateeName,
    String relationTypeCode,
    String statusCode, // 면담 상태 (미작성, DRAFT, COMPLETED)
    String contentSnippet // 내용 미리보기 (필요시)
) {
}
