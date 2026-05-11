package com.ees.eval.dto;

import lombok.Builder;
import java.util.Map;
import java.util.List;

@Builder
public record DashboardStatsDTO(
    long employeeCount,
    long departmentCount,
    String activePeriodName,
    int totalEvaluatees,
    int finalizedCount,
    double completionRate,
    Map<String, Long> gradeDistribution,
    Map<String, Double> deptAverageScores,
    List<RecentActivityDTO> recentActivities
) {}
