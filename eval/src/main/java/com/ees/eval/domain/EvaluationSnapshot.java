package com.ees.eval.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationSnapshot {
    private Long snapshotId;
    private Long periodId;
    private Long mappingId;
    private String snapshotData;
    private Integer snapshotVersion;
    private String sourceType;
    private LocalDateTime createdAt;
}
