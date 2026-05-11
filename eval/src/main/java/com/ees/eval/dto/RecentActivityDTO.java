package com.ees.eval.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record RecentActivityDTO(
    String evaluateeName,
    String deptName,
    String activityType, // e.g., "평가 확정", "평가 제출"
    LocalDateTime activityTime
) {}
