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
public class RegionPerformanceDTO {
    private String regionCode;
    private String regionName;
    private BigDecimal avgCompositeScore;
    private BigDecimal avgClaimCount;
    private BigDecimal avgRevenueGrowth;
    private BigDecimal avgHygieneScore;
    private BigDecimal avgCustomerScore;
}
