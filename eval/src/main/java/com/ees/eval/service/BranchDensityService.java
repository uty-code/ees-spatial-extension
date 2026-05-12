package com.ees.eval.service;

import com.ees.eval.dto.BranchDensityDTO;

public interface BranchDensityService {
    BranchDensityDTO getBranchDensity(double lat, double lng, double radiusMeters);
}
