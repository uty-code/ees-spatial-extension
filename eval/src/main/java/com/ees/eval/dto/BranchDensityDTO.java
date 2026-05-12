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
public class BranchDensityDTO {
    private int branchCount;
    private BigDecimal avgRevenueGrowth;
    private List<NearbyBranchDTO> nearbyBranches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NearbyBranchDTO {
        private Long branchId;
        private String branchName;
        private double distanceMeters;
        private BigDecimal revenueGrowth;
    }
}
