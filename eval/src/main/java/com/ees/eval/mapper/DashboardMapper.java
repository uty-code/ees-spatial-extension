package com.ees.eval.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    /**
     * 특정 차수의 전체 평가 대상자 수 조회
     */
    int countTotalEvaluatees(@Param("periodId") Long periodId);

    /**
     * 특정 차수의 최종 확정(FinalGrade 존재) 인원 수 조회
     */
    int countFinalizedEmployees(@Param("periodId") Long periodId);

    /**
     * 특정 차수의 등급 분포 조회
     * @return List of Maps with "grade_code" and "count"
     */
    List<Map<String, Object>> getGradeDistribution(@Param("periodId") Long periodId);

    /**
     * 특정 차수의 부서별 평균 점수 조회
     * @return List of Maps with "dept_name" and "avg_score"
     */
    List<Map<String, Object>> getDeptAverageScores(@Param("periodId") Long periodId);

    /**
     * 최근 최종 확정된 활동 상위 N건 조회
     */
    List<Map<String, Object>> getRecentFinalizedActivities(@Param("periodId") Long periodId, @Param("limit") int limit);
}
