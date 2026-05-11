package com.ees.eval.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record RecentActivityDTO(
    String evaluateeName,
    String deptName,
    String grade,
    String activityType,
    LocalDateTime activityTime
) {}
