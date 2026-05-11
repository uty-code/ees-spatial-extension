package com.ees.eval.service;

import com.ees.eval.dto.DashboardStatsDTO;

public interface DashboardService {
    /**
     * 현재 진행 중인 차수를 기준으로 대시보드 통계 데이터를 조회합니다.
     */
    DashboardStatsDTO getDashboardStats();
}
