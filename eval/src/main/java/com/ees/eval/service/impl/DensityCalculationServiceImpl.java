package com.ees.eval.service.impl;

import com.ees.eval.domain.RegionType;
import com.ees.eval.service.DensityCalculationService;
import org.springframework.stereotype.Service;

@Service
public class DensityCalculationServiceImpl implements DensityCalculationService {

    @Override
    public String determineDensityLevel(RegionType regionType, int nearbySameBrandCount) {
        if (regionType == RegionType.URBAN_CORE) {
            if (nearbySameBrandCount >= 9) return "HIGH";
            if (nearbySameBrandCount >= 5) return "MID";
            return "LOW";
        } else if (regionType == RegionType.GENERAL_CITY) {
            if (nearbySameBrandCount >= 6) return "HIGH";
            if (nearbySameBrandCount >= 3) return "MID";
            return "LOW";
        } else if (regionType == RegionType.SUBURBAN) {
            if (nearbySameBrandCount >= 4) return "HIGH";
            if (nearbySameBrandCount >= 2) return "MID";
            return "LOW";
        }
        return "LOW"; // Default fallback
    }
}
