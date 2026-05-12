package com.ees.eval.mapper;

import com.ees.eval.dto.EvaluationGapDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface EvaluationAnalysisMapper {
    List<EvaluationGapDTO> findGapAnalysisByPeriodId(@Param("periodId") Long periodId);
    
    List<java.util.Map<String, Object>> findTrendDataByEmpId(@Param("empId") Long empId, @Param("periodLimit") int limit);
    
    boolean existsSnapshotByMappingId(@Param("mappingId") Long mappingId);
}
