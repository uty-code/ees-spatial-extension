package com.ees.eval.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DensityDTO {
    private Long branchId;
    private Integer nearbySameBrandCount;
    private String densityLevel; // LOW, MID, HIGH
}
