package com.ees.eval.service;

import com.ees.eval.domain.RegionType;

public interface DensityCalculationService {
    String determineDensityLevel(RegionType regionType, int nearbySameBrandCount);
}
