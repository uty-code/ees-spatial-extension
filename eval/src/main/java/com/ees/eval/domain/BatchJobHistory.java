package com.ees.eval.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJobHistory {
    private Long jobId;
    private String jobName;
    private Long periodId;
    private String status; // READY, RUNNING, SUCCESS, FAILED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String resultMessage;
    private Integer version;
    private LocalDateTime createdAt;
    private Long createdBy;
}
