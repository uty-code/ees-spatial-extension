package com.ees.eval.controller;

import com.ees.eval.service.AutomatedEvaluationService;
import com.ees.eval.service.EvaluationAnalysisService;
import com.ees.eval.dto.EvaluationGapDTO;
import com.ees.eval.dto.ExecutiveReviewContextDTO;
import com.ees.eval.domain.EvaluationSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class OperationalEvaluationController {

    private final AutomatedEvaluationService automatedEvaluationService;
    private final EvaluationAnalysisService evaluationAnalysisService;

    /**
     * 특정 사원의 시스템 산출 운영 점수를 조회합니다.
     * 
     * @param empId 사원 ID
     * @param year 대상 연도
     * @param quarter 대상 분기
     * @return 운영 점수 정보
     */
    @GetMapping("/operational-score")
    public ResponseEntity<Map<String, Object>> getOperationalScore(
            @RequestParam Long empId,
            @RequestParam Integer year,
            @RequestParam Integer quarter) {
        
        BigDecimal score = automatedEvaluationService.calculateBranchKPIs(empId, year, quarter);
        
        Map<String, Object> response = new HashMap<>();
        response.put("empId", empId);
        response.put("year", year);
        response.put("quarter", quarter);
        response.put("operationalScore", score);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/gap-analysis")
    public List<EvaluationGapDTO> getGapAnalysis(@RequestParam Long periodId) {
        return evaluationAnalysisService.calculateGapAnalysis(periodId);
    }

    @GetMapping("/snapshots/{mappingId}")
    public ResponseEntity<EvaluationSnapshot> getSnapshot(@PathVariable Long mappingId) {
        return evaluationAnalysisService.getSnapshot(mappingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/executive-context")
    public ResponseEntity<Object> getExecutiveContext(
            @RequestParam Long periodId,
            @RequestParam(required = false) Long branchId) {
        
        List<ExecutiveReviewContextDTO> contexts = evaluationAnalysisService.getExecutiveReviewContext(periodId, branchId);
        
        if (branchId != null) {
            return contexts.stream()
                    .findFirst()
                    .map(dto -> ResponseEntity.ok((Object) dto))
                    .orElse(ResponseEntity.notFound().build());
        }
        
        return ResponseEntity.ok(contexts);
    }
}
