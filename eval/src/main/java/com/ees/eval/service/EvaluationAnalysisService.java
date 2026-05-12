package com.ees.eval.service;

import com.ees.eval.dto.EvaluationGapDTO;
import com.ees.eval.dto.ExecutiveReviewContextDTO;
import com.ees.eval.domain.EvaluationSnapshot;
import java.util.List;
import java.util.Optional;

public interface EvaluationAnalysisService {
    List<EvaluationGapDTO> calculateGapAnalysis(Long periodId);
    List<ExecutiveReviewContextDTO> getExecutiveReviewContext(Long periodId, Long branchId);
    Optional<EvaluationSnapshot> getSnapshot(Long mappingId);
}
