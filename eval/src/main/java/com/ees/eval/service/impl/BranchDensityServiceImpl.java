package com.ees.eval.service.impl;

import com.ees.eval.dto.BranchDensityDTO;
import com.ees.eval.mapper.BranchDensityMapper;
import com.ees.eval.service.BranchDensityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchDensityServiceImpl implements BranchDensityService {

    private final BranchDensityMapper branchDensityMapper;

    @Override
    @Transactional(readOnly = true)
    public BranchDensityDTO getBranchDensity(double lat, double lng, double radiusMeters) {
        log.info("Calculating branch density for coordinates ({}, {}) within {} meters", lat, lng, radiusMeters);
        
        List<BranchDensityDTO.NearbyBranchDTO> nearbyBranches = branchDensityMapper.selectNearbyBranches(lat, lng, radiusMeters);

        BigDecimal avgGrowth = BigDecimal.ZERO;
        if (!nearbyBranches.isEmpty()) {
            BigDecimal totalGrowth = nearbyBranches.stream()
                    .map(BranchDensityDTO.NearbyBranchDTO::getRevenueGrowth)
                    .filter(growth -> growth != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long countWithGrowth = nearbyBranches.stream()
                    .filter(b -> b.getRevenueGrowth() != null)
                    .count();

            if (countWithGrowth > 0) {
                avgGrowth = totalGrowth.divide(BigDecimal.valueOf(countWithGrowth), 2, RoundingMode.HALF_UP);
            }
        }

        return BranchDensityDTO.builder()
                .branchCount(nearbyBranches.size())
                .avgRevenueGrowth(avgGrowth)
                .nearbyBranches(nearbyBranches)
                .build();
    }
}
