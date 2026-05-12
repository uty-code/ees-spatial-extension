package com.ees.eval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerPerformanceDTO {
    private Long empId;
    private BigDecimal avgCompositeScore;
    private BigDecimal avgRevenueGrowth;
    private BigDecimal avgHygieneScore;
    private BigDecimal avgCustomerScore;
    private BigDecimal totalClaimCount;
    private List<BranchQuarterlyPerformanceDTO> quarterlyTrends;
}
