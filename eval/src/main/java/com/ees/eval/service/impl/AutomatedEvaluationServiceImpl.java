package com.ees.eval.service.impl;

import com.ees.eval.dto.ManagerPerformanceDTO;
import com.ees.eval.service.AutomatedEvaluationService;
import com.ees.eval.service.EvaluationPeriodService;
import com.ees.eval.service.ManagerPerformanceService;
import com.ees.eval.service.ScoreCalculationService;
import com.ees.eval.mapper.EvaluatorMappingMapper;
import com.ees.eval.mapper.EvaluationMapper;
import com.ees.eval.mapper.EvaluationElementMapper;
import com.ees.eval.mapper.EvaluationSnapshotMapper;
import com.ees.eval.service.SpatialAnalysisService;
import com.ees.eval.dto.DensityDTO;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomatedEvaluationServiceImpl implements AutomatedEvaluationService {

    private final ManagerPerformanceService managerPerformanceService;
    private final EvaluationPeriodService evaluationPeriodService;
    private final EvaluatorMappingMapper evaluatorMappingMapper;
    private final EvaluationMapper evaluationMapper;
    private final EvaluationElementMapper evaluationElementMapper;
    private final ScoreCalculationService scoreCalculationService;
    private final EvaluationSnapshotMapper evaluationSnapshotMapper;
    private final ObjectMapper objectMapper;
    private final SpatialAnalysisService spatialAnalysisService;

    private static final Long SYSTEM_EVALUATOR_ID = 1000L; // System Admin
    private static final String RELATION_TYPE_OPERATIONAL = "OPERATIONAL";

    @Override
    public BigDecimal calculateBranchKPIs(Long empId, Integer year, Integer quarter) {
        // 1. Fetch Raw Performance
        ManagerPerformanceDTO performance = managerPerformanceService.getManagerPerformance(empId, year, quarter);
        if (performance == null || performance.getAvgCompositeScore() == null) {
            return BigDecimal.ZERO;
        }

        // 2. Calculate Base Score using policy service
        BigDecimal baseScore = scoreCalculationService.calculateOperationalScore(performance.getAvgCompositeScore());

        // 3. Spatial Intelligence: Apply Difficulty Correction
        DensityDTO density = spatialAnalysisService.calculateDensityByEmpId(empId);
        BigDecimal coefficient = spatialAnalysisService.getDifficultyCoefficient(density.getDensityLevel());
        BigDecimal correctedScore = baseScore.multiply(coefficient).setScale(1, RoundingMode.HALF_UP);

        log.info("Spatial Correction applied: empId={}, base={}, level={}, coeff={}, final={}", 
                empId, baseScore, density.getDensityLevel(), coefficient, correctedScore);
        
        return correctedScore;
    }

    @Override
    @Transactional
    public void populateSystemEvaluation(Integer year, Integer quarter, Long evaluateeId) {
        BigDecimal correctedScore = calculateBranchKPIs(evaluateeId, year, quarter);
        DensityDTO density = spatialAnalysisService.calculateDensityByEmpId(evaluateeId);
        
        java.util.List<com.ees.eval.dto.EvaluationPeriodDTO> inProgressPeriods = evaluationPeriodService.getInProgressPeriods();
        if (inProgressPeriods.isEmpty()) {
            throw new IllegalStateException("No active evaluation period found.");
        }

        com.ees.eval.dto.EvaluationPeriodDTO currentPeriod = inProgressPeriods.get(0);
        Long periodId = currentPeriod.periodId();

        // 1. Create/Check Evaluator Mapping for OPERATIONAL type (Idempotency)
        com.ees.eval.domain.EvaluatorMapping mapping = evaluatorMappingMapper.findByUniqueKey(periodId, evaluateeId, SYSTEM_EVALUATOR_ID, RELATION_TYPE_OPERATIONAL)
                .orElse(null);
        
        Long mappingId;
        if (mapping == null) {
            mapping = com.ees.eval.domain.EvaluatorMapping.builder()
                    .periodId(periodId)
                    .evaluateeId(evaluateeId)
                    .evaluatorId(SYSTEM_EVALUATOR_ID)
                    .relationTypeCode(RELATION_TYPE_OPERATIONAL)
                    .build();
            evaluatorMappingMapper.insert(mapping);
            mappingId = mapping.getMappingId();
        } else {
            mappingId = mapping.getMappingId();
        }

        // 2. Find "Operational KPI" element for this period
        com.ees.eval.domain.EvaluationElement kpiElement = evaluationElementMapper.findByPeriodId(periodId, null)
                .stream()
                .filter(e -> e.getElementName().contains("운영") || e.getElementName().contains("KPI"))
                .findFirst()
                .orElse(null);

        if (kpiElement != null) {
            // 3. Insert or Update Evaluation result
            com.ees.eval.domain.Evaluation evaluation = com.ees.eval.domain.Evaluation.builder()
                    .mappingId(mappingId)
                    .elementId(kpiElement.getElementId())
                    .score(correctedScore.intValue())
                    .reason("시스템 자동 집계 점수 (공간 보정 적용: " + density.getDensityLevel() + ")")
                    .confirmStatusCode("SUBMITTED")
                    .build();
            
            java.util.Optional<com.ees.eval.domain.Evaluation> existingEval = evaluationMapper.findByMappingIdAndElementId(mappingId, kpiElement.getElementId());
            if (existingEval.isPresent()) {
                evaluation.setEvalId(existingEval.get().getEvalId());
                evaluationMapper.update(evaluation);
            } else {
                evaluationMapper.insert(evaluation);
            }

            // 4. Create Data Snapshot (Audit Trail)
            saveEvaluationSnapshot(currentPeriod, mappingId, evaluateeId, year, quarter, correctedScore);
        }
    }

    private void saveEvaluationSnapshot(com.ees.eval.dto.EvaluationPeriodDTO period, Long mappingId, Long evaluateeId, Integer year, Integer quarter, BigDecimal score) {
        try {
            ManagerPerformanceDTO performance = managerPerformanceService.getManagerPerformance(evaluateeId, year, quarter);
            DensityDTO density = spatialAnalysisService.calculateDensityByEmpId(evaluateeId);
            
            java.util.Map<String, Object> snapshotMap = new java.util.HashMap<>();
            snapshotMap.put("empId", evaluateeId);
            snapshotMap.put("year", year);
            snapshotMap.put("quarter", quarter);
            snapshotMap.put("source", "BATCH");
            snapshotMap.put("metrics", performance);
            snapshotMap.put("calculatedScore", score);
            snapshotMap.put("calculatedAt", java.time.LocalDateTime.now().toString());

            String jsonData = objectMapper.writeValueAsString(snapshotMap);

            com.ees.eval.domain.EvaluationSnapshot snapshot = com.ees.eval.domain.EvaluationSnapshot.builder()
                    .periodId(period.periodId())
                    .mappingId(mappingId)
                    .snapshotData(jsonData)
                    .snapshotVersion(1)
                    .sourceType("BATCH")
                    .build();

            evaluationSnapshotMapper.insert(snapshot);
            log.info("[Snapshot] Saved KPI snapshot for evaluateeId={}, mappingId={}", evaluateeId, mappingId);
        } catch (Exception e) {
            log.error("[Snapshot] Failed to save evaluation snapshot: {}", e.getMessage(), e);
            throw new RuntimeException("Snapshot saving failed, rolling back evaluation.", e);
        }
    }
}
