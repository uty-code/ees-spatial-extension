package com.ees.eval.service;

import com.ees.eval.dto.ManagerPerformanceDTO;

public interface ManagerPerformanceService {
    ManagerPerformanceDTO getPerformanceByManager(Long empId);
    ManagerPerformanceDTO getManagerPerformance(Long empId, Integer year, Integer quarter);
}
