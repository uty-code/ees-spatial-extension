package com.ees.eval.service.impl;

import com.ees.eval.dto.ManagerPerformanceDTO;
import com.ees.eval.mapper.ManagerPerformanceMapper;
import com.ees.eval.service.ManagerPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerPerformanceServiceImpl implements ManagerPerformanceService {

    private final ManagerPerformanceMapper managerPerformanceMapper;

    @Override
    @Transactional(readOnly = true)
    public ManagerPerformanceDTO getPerformanceByManager(Long empId) {
        log.info("Fetching performance data for manager: {}", empId);
        return managerPerformanceMapper.selectPerformanceByManager(empId);
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerPerformanceDTO getManagerPerformance(Long empId, Integer year, Integer quarter) {
        log.info("Fetching performance data for manager: {} for period: {}-Q{}", empId, year, quarter);
        return managerPerformanceMapper.selectPerformanceByManagerAndPeriod(empId, year, quarter);
    }
}
