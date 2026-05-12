package com.ees.eval.service;

import com.ees.eval.dto.RegionPerformanceDTO;
import java.util.List;

public interface BranchPerformanceService {
    List<RegionPerformanceDTO> getPerformanceByRegion();
}
