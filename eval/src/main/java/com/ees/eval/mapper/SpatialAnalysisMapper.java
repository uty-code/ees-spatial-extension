package com.ees.eval.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SpatialAnalysisMapper {
    /**
     * 특정 지점 반경 내의 동일 브랜드 매장 수를 계산합니다.
     * 
     * @param branchId 기준 지점 ID
     * @param radius 반경 (미터 단위)
     * @return 인근 동일 브랜드 매장 수
     */
    int countNearbySameBrandBranches(@Param("branchId") Long branchId, @Param("radius") double radius);

    /**
     * 특정 지점의 Region Type을 조회합니다.
     */
    com.ees.eval.domain.RegionType findRegionTypeByBranchId(@Param("branchId") Long branchId);

    /**
     * 특정 사원이 담당하는 지점 ID를 조회합니다.
     * 
     * @param empId 사원 ID
     * @return 지점 ID (없으면 null)
     */
    Long findBranchIdByEmpId(@Param("empId") Long empId);

    /**
     * 특정 차수의 모든 지점 공간 정보 및 성과 점수를 조회합니다.
     */
    java.util.List<com.ees.eval.dto.HeatMapDTO> findAllBranchSpatialData(@Param("periodId") Long periodId);
}
