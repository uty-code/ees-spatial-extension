package com.ees.eval.service.impl;

import com.ees.eval.dto.DensityDTO;
import com.ees.eval.dto.HeatMapDTO;
import com.ees.eval.dto.EvaluationGapDTO;
import com.ees.eval.mapper.SpatialAnalysisMapper;
import com.ees.eval.service.SpatialAnalysisService;
import com.ees.eval.service.EvaluationAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpatialAnalysisServiceImpl implements SpatialAnalysisService {

    private final SpatialAnalysisMapper spatialAnalysisMapper;
    private final EvaluationAnalysisService evaluationAnalysisService;

    public SpatialAnalysisServiceImpl(SpatialAnalysisMapper spatialAnalysisMapper, @Lazy EvaluationAnalysisService evaluationAnalysisService) {
        this.spatialAnalysisMapper = spatialAnalysisMapper;
        this.evaluationAnalysisService = evaluationAnalysisService;
    }

    private static final double DEFAULT_RADIUS = 500.0; // 500 meters

    @Override
    public DensityDTO calculateDensity(Long branchId) {
        int count = spatialAnalysisMapper.countNearbySameBrandBranches(branchId, DEFAULT_RADIUS);
        
        String level;
        if (count >= 6) {
            level = "HIGH";
        } else if (count >= 3) {
            level = "MID";
        } else {
            level = "LOW";
        }

        return DensityDTO.builder()
                .branchId(branchId)
                .nearbySameBrandCount(count)
                .densityLevel(level)
                .build();
    }

    @Override
    public DensityDTO calculateDensityByEmpId(Long empId) {
        Long branchId = spatialAnalysisMapper.findBranchIdByEmpId(empId);
        if (branchId == null) {
            return DensityDTO.builder()
                    .nearbySameBrandCount(0)
                    .densityLevel("LOW")
                    .build();
        }
        return calculateDensity(branchId);
    }

    @Override
    public BigDecimal getDifficultyCoefficient(String densityLevel) {
        switch (densityLevel) {
            case "HIGH": return new BigDecimal("1.05");
            case "MID": return new BigDecimal("1.02");
            default: return new BigDecimal("1.00");
        }
    }

    @Override
    public List<HeatMapDTO> getHeatMapData(Long periodId) {
        List<HeatMapDTO> spatialData = spatialAnalysisMapper.findAllBranchSpatialData(periodId);
        List<EvaluationGapDTO> gaps = evaluationAnalysisService.calculateGapAnalysis(periodId);
        
        Map<Long, String> riskMap = gaps.stream()
                .collect(Collectors.toMap(EvaluationGapDTO::getEmpId, EvaluationGapDTO::getStatus));

        return spatialData.stream().map(dto -> {
            Long branchId = dto.getBranchId();
            Long empId = dto.getEmpId();
            DensityDTO density = calculateDensity(branchId);

            dto.setDensityLevel(density.getDensityLevel());
            dto.setRiskLevel(riskMap.getOrDefault(empId, "NORMAL"));
            return dto;
        }).collect(Collectors.toList());
    }
}
