package com.ees.eval.mapper;

import com.ees.eval.domain.EvaluationSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface EvaluationSnapshotMapper {
    int insert(EvaluationSnapshot snapshot);
    Optional<EvaluationSnapshot> findByMappingId(@Param("mappingId") Long mappingId);
    List<EvaluationSnapshot> findByPeriodId(@Param("periodId") Long periodId);
}
