package com.ees.eval.service.impl;

import com.ees.eval.dto.DensityDTO;
import com.ees.eval.dto.EvaluationGapDTO;
import com.ees.eval.dto.ExecutiveReviewContextDTO;
import com.ees.eval.domain.EvaluationSnapshot;
import com.ees.eval.mapper.EvaluationAnalysisMapper;
import com.ees.eval.mapper.EvaluationSnapshotMapper;
import com.ees.eval.mapper.EvaluatorMappingMapper;
import com.ees.eval.service.EvaluationAnalysisService;
import com.ees.eval.service.SpatialAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationAnalysisServiceImpl implements EvaluationAnalysisService {

    private final EvaluationAnalysisMapper evaluationAnalysisMapper;
    private final EvaluationSnapshotMapper evaluationSnapshotMapper;
    private final EvaluatorMappingMapper evaluatorMappingMapper;
    private final SpatialAnalysisService spatialAnalysisService;

    private static final BigDecimal GAP_THRESHOLD = new BigDecimal("15.0");

    @Override
    public List<EvaluationGapDTO> calculateGapAnalysis(Long periodId) {
        List<EvaluationGapDTO> gaps = evaluationAnalysisMapper.findGapAnalysisByPeriodId(periodId);

        return gaps.stream().map(dto -> {
            BigDecimal op = dto.getOperationalScore();
            BigDecimal ma = dto.getManagerScore();
            
            // 프론트엔드 매핑용 필드 설정
            dto.setBaseScore(op != null ? op : BigDecimal.ZERO);
            
            if (op != null && ma != null) {
                BigDecimal gapValue = ma.subtract(op).abs();
                dto.setGap(ma.subtract(op)); // Actual gap (Manager - Operational)
                
                // 최종 점수 계산 (60:40 가중치)
                BigDecimal finalScore = op.multiply(new BigDecimal("0.6"))
                        .add(ma.multiply(new BigDecimal("0.4")))
                        .setScale(1, RoundingMode.HALF_UP);
                dto.setFinalScore(finalScore);
                
                if (gapValue.compareTo(new BigDecimal("30.0")) >= 0) {
                    dto.setStatus("ANOMALY");
                } else if (gapValue.compareTo(GAP_THRESHOLD) >= 0) {
                    dto.setStatus("WARNING");
                } else {
                    dto.setStatus("NORMAL");
                }
            } else {
                dto.setFinalScore(op != null ? op : BigDecimal.ZERO);
                dto.setStatus("PENDING");
                dto.setGap(BigDecimal.ZERO);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ExecutiveReviewContextDTO> getExecutiveReviewContext(Long periodId, Long branchId) {
        List<EvaluationGapDTO> gaps = calculateGapAnalysis(periodId);

        return gaps.stream()
                .filter(gapDto -> branchId == null || branchId.equals(spatialAnalysisService.calculateDensityByEmpId(gapDto.getEmpId()).getBranchId()))
                .map(gapDto -> {
            // 1. Calculate Final Score (60% Operational + 40% Manager)
            BigDecimal finalScore = dtoFinalScore(gapDto);

            // 2. Trend Aggregation (Last 4 periods)
            List<Map<String, Object>> trendData = evaluationAnalysisMapper.findTrendDataByEmpId(gapDto.getEmpId(), 4);
            Map<String, BigDecimal> trendMap = trendData.stream()
                    .collect(Collectors.toMap(
                            m -> (String) m.get("periodName"),
                            m -> new BigDecimal(m.get("score").toString()),
                            (v1, v2) -> v1, // Keep newest if duplicate
                            java.util.LinkedHashMap::new
                    ));

            // 3. Risk Level Assignment
            String riskLevel = "LOW";
            if ("ANOMALY".equals(gapDto.getStatus())) {
                riskLevel = "HIGH";
            } else if ("WARNING".equals(gapDto.getStatus())) {
                riskLevel = "MEDIUM";
            }

            // 4. Spatial Context
            DensityDTO density = spatialAnalysisService.calculateDensityByEmpId(gapDto.getEmpId());
            BigDecimal coefficient = spatialAnalysisService.getDifficultyCoefficient(density.getDensityLevel());

            // 5. Snapshot check
            Long opMappingId = evaluatorMappingMapper.findByUniqueKey(periodId, gapDto.getEmpId(), 1000L, "OPERATIONAL")
                    .map(com.ees.eval.domain.EvaluatorMapping::getMappingId)
                    .orElse(null);
            boolean snapshotExists = opMappingId != null && evaluationAnalysisMapper.existsSnapshotByMappingId(opMappingId);

            return ExecutiveReviewContextDTO.builder()
                    .empId(gapDto.getEmpId())
                    .empName(gapDto.getEmpName())
                    .branchName(gapDto.getBranchName()) // 추가: 지점명 매핑
                    .finalScore(finalScore)
                    .riskLevel(riskLevel)
                    .gap(gapDto.getGap())
                    .snapshotAvailable(snapshotExists)
                    .densityLevel(density.getDensityLevel())
                    .nearbySameBrandCount(density.getNearbySameBrandCount())
                    .difficultyCoefficient(coefficient)
                    .spatialAdjusted(coefficient.compareTo(BigDecimal.ONE) > 0)
                    .trends(trendMap)
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal dtoFinalScore(EvaluationGapDTO gapDto) {
        BigDecimal finalScore = BigDecimal.ZERO;
        if (gapDto.getOperationalScore() != null && gapDto.getManagerScore() != null) {
            finalScore = gapDto.getOperationalScore().multiply(new BigDecimal("0.6"))
                    .add(gapDto.getManagerScore().multiply(new BigDecimal("0.4")))
                    .setScale(1, RoundingMode.HALF_UP);
        } else if (gapDto.getOperationalScore() != null) {
            finalScore = gapDto.getOperationalScore();
        }
        return finalScore;
    }

    @Override
    public Optional<EvaluationSnapshot> getSnapshot(Long mappingId) {
        return evaluationSnapshotMapper.findByMappingId(mappingId);
    }
}
