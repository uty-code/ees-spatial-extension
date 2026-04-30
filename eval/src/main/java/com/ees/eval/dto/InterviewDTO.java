package com.ees.eval.dto;

import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 면담 기록(정성평가) 데이터를 전달하기 위한 DTO입니다.
 */
@Builder
public record InterviewDTO(
        Long interviewId,
        Long mappingId,
        String content1,
        String content2,
        String content3,
        String content4,
        String statusCode,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
