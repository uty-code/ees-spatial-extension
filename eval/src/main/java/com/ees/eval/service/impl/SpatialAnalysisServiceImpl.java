package com.ees.eval.service.impl;

import com.ees.eval.domain.RegionType;
import com.ees.eval.dto.DensityDTO;
import com.ees.eval.dto.HeatMapDTO;
import com.ees.eval.dto.EvaluationGapDTO;
import com.ees.eval.mapper.SpatialAnalysisMapper;
import com.ees.eval.service.DensityCalculationService;
import com.ees.eval.service.DifficultyEvaluationService;
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
    private final DensityCalculationService densityCalculationService;
    private final DifficultyEvaluationService difficultyEvaluationService;

    public SpatialAnalysisServiceImpl(
            SpatialAnalysisMapper spatialAnalysisMapper, 
            @Lazy EvaluationAnalysisService evaluationAnalysisService,
            DensityCalculationService densityCalculationService,
            DifficultyEvaluationService difficultyEvaluationService) {
        this.spatialAnalysisMapper = spatialAnalysisMapper;
        this.evaluationAnalysisService = evaluationAnalysisService;
        this.densityCalculationService = densityCalculationService;
        this.difficultyEvaluationService = difficultyEvaluationService;
    }

    @Override
    public DensityDTO calculateDensity(Long branchId) {
        RegionType regionType = spatialAnalysisMapper.findRegionTypeByBranchId(branchId);
        if (regionType == null) {
            regionType = RegionType.GENERAL_CITY; // Fallback
        }
        
        int radius = regionType.getDefaultRadius();
        int count = spatialAnalysisMapper.countNearbySameBrandBranches(branchId, radius);
        
        String level = densityCalculationService.determineDensityLevel(regionType, count);
        BigDecimal difficultyCoefficient = difficultyEvaluationService.getDifficultyCoefficient(level);

        return DensityDTO.builder()
                .branchId(branchId)
                .regionType(regionType)
                .radius(radius)
                .nearbySameBrandCount(count)
                .densityLevel(level)
                .difficultyCoefficient(difficultyCoefficient)
                .build();
    }

    @Override
    public DensityDTO calculateDensityByEmpId(Long empId) {
        Long branchId = spatialAnalysisMapper.findBranchIdByEmpId(empId);
        if (branchId == null) {
            return DensityDTO.builder()
                    .nearbySameBrandCount(0)
                    .densityLevel("LOW")
                    .difficultyCoefficient(new BigDecimal("1.00"))
                    .build();
        }
        return calculateDensity(branchId);
    }

    @Override
    public BigDecimal getDifficultyCoefficient(String densityLevel) {
        return difficultyEvaluationService.getDifficultyCoefficient(densityLevel);
    }

    @Override
    public BigDecimal calculateCappedScore(BigDecimal baseScore, BigDecimal coefficient, String densityLevel) {
        return difficultyEvaluationService.calculateCappedScore(baseScore, coefficient, densityLevel);
    }

    @Override
    public List<HeatMapDTO> getHeatMapData(Long periodId) {
        List<HeatMapDTO> spatialData = spatialAnalysisMapper.findAllBranchSpatialData(periodId);
        List<EvaluationGapDTO> gaps = evaluationAnalysisService.calculateGapAnalysis(periodId);
        
        Map<Long, String> riskMap = gaps.stream()
                .collect(Collectors.toMap(
                        EvaluationGapDTO::getEmpId, 
                        EvaluationGapDTO::getStatus,
                        (v1, v2) -> v1
                ));

        return spatialData.stream().map(dto -> {
            Long branchId = dto.getBranchId();
            Long empId = dto.getEmpId();
            
            // regionType is already in dto from the mapper.
            RegionType regionType = dto.getRegionType();
            if (regionType == null) regionType = RegionType.GENERAL_CITY;
            
            int radius = regionType.getDefaultRadius();
            int count = spatialAnalysisMapper.countNearbySameBrandBranches(branchId, radius);
            String level = densityCalculationService.determineDensityLevel(regionType, count);

            dto.setDensityLevel(level);
            dto.setRiskLevel(riskMap.getOrDefault(empId, "NORMAL"));
            return dto;
        }).collect(Collectors.toList());
    }
}
