package com.ees.eval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchQuarterlyPerformanceDTO {
    private int perfYear;
    private int perfQuarter;
    private BigDecimal revenueGrowth;
    private BigDecimal hygieneScore;
    private int claimCount;
    private BigDecimal customerScore;
    private BigDecimal compositeScore;
}
