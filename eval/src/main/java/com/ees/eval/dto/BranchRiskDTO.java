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
public class BranchRiskDTO {
    private Long branchId;
    private String branchName;
    private String address;
    private String regionCode;
    private BigDecimal revenueGrowth;
    private int claimCount;
    private String riskReason;
}
