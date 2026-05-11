package com.ees.eval.service;

import com.ees.eval.dto.DashboardStatsDTO;
import com.ees.eval.dto.EmployeeDashboardDTO;

public interface DashboardService {
    /**
     * 현재 진행 중인 차수를 기준으로 대시보드 통계 데이터를 조회합니다.
     */
    DashboardStatsDTO getDashboardStats();

    /**
     * 특정 사원의 대시보드 통계 데이터를 조회합니다.
     */
    EmployeeDashboardDTO getEmployeeDashboardStats(Long empId);
}
