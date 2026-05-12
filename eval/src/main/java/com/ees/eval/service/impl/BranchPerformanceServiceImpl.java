package com.ees.eval.service.impl;

import com.ees.eval.dto.RegionPerformanceDTO;
import com.ees.eval.mapper.BranchPerformanceMapper;
import com.ees.eval.service.BranchPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchPerformanceServiceImpl implements BranchPerformanceService {

    private final BranchPerformanceMapper branchPerformanceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RegionPerformanceDTO> getPerformanceByRegion() {
        log.info("Fetching performance data by region");
        return branchPerformanceMapper.selectPerformanceByRegion();
    }
}
