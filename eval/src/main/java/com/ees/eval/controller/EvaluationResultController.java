package com.ees.eval.controller;

import com.ees.eval.domain.Department;
import com.ees.eval.domain.Employee;
import com.ees.eval.domain.Evaluation;
import com.ees.eval.domain.EvaluatorMapping;
import com.ees.eval.domain.FinalGrade;
import com.ees.eval.dto.EvaluationElementDTO;
import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.dto.EvaluationResultDTO;
import com.ees.eval.dto.EvaluationTypeWeightDTO;
import com.ees.eval.mapper.DepartmentMapper;
import com.ees.eval.mapper.EmployeeMapper;
import com.ees.eval.mapper.EvaluationMapper;
import com.ees.eval.mapper.EvaluatorMappingMapper;
import com.ees.eval.mapper.FinalGradeMapper;
import com.ees.eval.service.EvaluationElementService;
import com.ees.eval.service.EvaluationPeriodService;
import com.ees.eval.service.EvaluationTypeWeightService;
import com.ees.eval.service.ScoreCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 평가 결과 현황 컨트롤러입니다.
 * 최종 확정(EXECUTIVE 제출)이 완료된 사원의 종합 점수와 등급을 조회합니다.
 */
@Slf4j
@Controller
@RequestMapping("/eval/result")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EXECUTIVE')")
public class EvaluationResultController {

    private final EvaluationPeriodService periodService;
    private final EvaluatorMappingMapper mappingMapper;
    private final EvaluationMapper evaluationMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final FinalGradeMapper finalGradeMapper;
    private final EvaluationElementService elementService;
    private final EvaluationTypeWeightService typeWeightService;
    private final ScoreCalculationService scoreCalculationService;

    @GetMapping
    public String list(@RequestParam(name = "periodId", required = false) Long periodId,
                       @RequestParam(name = "deptId", required = false) Long deptId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {

        model.addAttribute("activeMenu", "eval-result");
        Long loginEmpId = Long.parseLong(userDetails.getUsername());

        // 1. 차수 목록 조회 (PLANNED 제외 — 진행 중 또는 완료된 차수만 표시)
        List<EvaluationPeriodDTO> periods = periodService.getAllPeriods().stream()
                .filter(p -> !"PLANNED".equals(p.statusCode()))
                .collect(Collectors.toList());
        model.addAttribute("periods", periods);

        EvaluationPeriodDTO selectedPeriod;
        if (periodId != null) {
            selectedPeriod = periodService.getPeriodById(periodId);
        } else {
            selectedPeriod = periods.stream()
                    .filter(p -> "IN_PROGRESS".equals(p.statusCode()))
                    .findFirst()
                    .orElse(periods.isEmpty() ? null : periods.get(0));
        }
        model.addAttribute("selectedPeriod", selectedPeriod);

        if (selectedPeriod == null) {
            model.addAttribute("infoMessage", "진행 중인 평가 차수가 없습니다.");
            return "eval/result/list";
        }

        // 2. 부서 목록 (필터용) - 관리자/임원은 전체, 부서장은 자기 부서만
        List<Department> departments = departmentMapper.findAll();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isExecutive = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EXECUTIVE"));

        if (isAdmin || isExecutive) {
            model.addAttribute("departments", departments);
        } else {
            // 부서장인 경우 자기 부서만
            Employee loginEmp = employeeMapper.findById(loginEmpId).orElse(null);
            if (loginEmp != null) {
                deptId = loginEmp.getDeptId();
                final Long managerDeptId = deptId;
                departments = departments.stream()
                        .filter(d -> d.getDeptId().equals(managerDeptId))
                        .collect(Collectors.toList());
                model.addAttribute("departments", departments);
            }
        }
        model.addAttribute("selectedDeptId", deptId);

        // 3. 최종 확정된 사원 목록 조회 (final_grades_51에 데이터가 있는 사원만)
        List<FinalGrade> finalGrades = finalGradeMapper.findByPeriodId(selectedPeriod.periodId());
        if (finalGrades.isEmpty()) {
            model.addAttribute("infoMessage", "최종 확정된 평가 결과가 없습니다.");
            return "eval/result/list";
        }

        // 4. 확정된 사원 ID 목록 추출
        List<Long> confirmedEmpIds = finalGrades.stream()
                .map(FinalGrade::getEmpId)
                .distinct()
                .collect(Collectors.toList());

        // 5. 사원 정보 벌크 조회
        Map<Long, Employee> employeeMap = employeeMapper.findByIds(confirmedEmpIds).stream()
                .collect(Collectors.toMap(Employee::getEmpId, e -> e, (a, b) -> a));

        // 6. 부서 필터 적용
        final Long filterDeptId = deptId;
        if (filterDeptId != null) {
            confirmedEmpIds = confirmedEmpIds.stream()
                    .filter(empId -> {
                        Employee emp = employeeMap.get(empId);
                        return emp != null && filterDeptId.equals(emp.getDeptId());
                    })
                    .collect(Collectors.toList());
        }

        // 7. 결과 DTO 조립 — 벌크 데이터를 활용하여 유형별 점수 계산
        Map<Long, FinalGrade> gradeMap = finalGrades.stream()
                .collect(Collectors.toMap(FinalGrade::getEmpId, g -> g, (a, b) -> a));

        // ========== [최적화] 유형별 점수 계산에 필요한 데이터 일괄 조회 ==========
        // (A) 확정 사원들의 모든 매핑 일괄 조회 (1회)
        List<EvaluatorMapping> allMappings = !confirmedEmpIds.isEmpty()
                ? mappingMapper.findByEvaluateeIds(selectedPeriod.periodId(), confirmedEmpIds)
                : Collections.emptyList();

        // EXECUTIVE 매핑만 추출 (유형별 점수 계산용)
        Map<Long, EvaluatorMapping> execMappingByEmpId = allMappings.stream()
                .filter(m -> "EXECUTIVE".equals(m.getRelationTypeCode()) && "n".equals(m.getIsDeleted()))
                .collect(Collectors.toMap(EvaluatorMapping::getEvaluateeId, m -> m, (a, b) -> a));

        // (B) EXECUTIVE 매핑들의 평가 데이터 일괄 조회 (1회)
        List<Long> execMappingIds = execMappingByEmpId.values().stream()
                .map(EvaluatorMapping::getMappingId)
                .collect(Collectors.toList());
        Map<Long, List<Evaluation>> execEvalGroupMap = !execMappingIds.isEmpty()
                ? evaluationMapper.findByMappingIds(execMappingIds).stream()
                        .collect(Collectors.groupingBy(Evaluation::getMappingId))
                : Collections.emptyMap();

        // (C) 평가 요소 부서별 캐싱 (부서당 1회만 조회)
        Map<Long, List<EvaluationElementDTO>> elementCacheByDeptId = new HashMap<>();

        List<EvaluationResultDTO> results = new ArrayList<>();

        for (Long empId : confirmedEmpIds) {
            Employee emp = employeeMap.get(empId);
            if (emp == null) continue;

            FinalGrade fg = gradeMap.get(empId);
            Integer totalScore = (fg != null) ? fg.getTotalScore() : null;
            String gradeCode = (fg != null) ? fg.getFinalGradeCode() : null;

            // 총점이 없으면 계산 시도
            if (totalScore == null) {
                totalScore = scoreCalculationService.calculateTotalScore(selectedPeriod.periodId(), empId);
                if (totalScore != null) {
                    gradeCode = scoreCalculationService.determineGrade(totalScore);
                }
            }

            // 유형별 점수 계산 (사전 조회된 데이터로 인메모리 계산)
            Long empDeptId = emp.getDeptId();
            EvaluatorMapping execMapping = execMappingByEmpId.get(empId);
            Integer perfScore = null;
            Integer compScore = null;
            Integer multiScore = null;

            if (execMapping != null) {
                List<Evaluation> execEvals = execEvalGroupMap.getOrDefault(execMapping.getMappingId(), Collections.emptyList())
                        .stream()
                        .filter(e -> "SUBMITTED".equals(e.getConfirmStatusCode()))
                        .collect(Collectors.toList());

                if (!execEvals.isEmpty()) {
                    Long cacheKey = empDeptId != null ? empDeptId : -1L;
                    List<EvaluationElementDTO> allElements = elementCacheByDeptId.computeIfAbsent(cacheKey,
                            k -> getElementsWithFallback(selectedPeriod.periodId(), empDeptId));

                    Map<Long, Evaluation> evalMap = execEvals.stream()
                            .collect(Collectors.toMap(Evaluation::getElementId, e -> e, (a, b) -> a));

                    perfScore = calculateTypeScoreInMemory(allElements, evalMap, "PERFORMANCE");
                    compScore = calculateTypeScoreInMemory(allElements, evalMap, "COMPETENCY");
                    multiScore = calculateTypeScoreInMemory(allElements, evalMap, "MULTI_DIMENSIONAL");
                }
            }

            results.add(EvaluationResultDTO.builder()
                    .empId(empId)
                    .empName(emp.getName())
                    .deptName(emp.getDeptName())
                    .positionName(emp.getPositionName())
                    .performanceScore(perfScore)
                    .competencyScore(compScore)
                    .multiDimensionalScore(multiScore)
                    .totalScore(totalScore)
                    .gradeCode(gradeCode)
                    .isConfirmed(fg != null)
                    .build());
        }

        // 8. 직급 기준 정렬 (높은 직급 우선, positionId 기준)
        results.sort((a, b) -> {
            Employee empA = employeeMap.get(a.empId());
            Employee empB = employeeMap.get(b.empId());
            if (empA == null || empB == null) return 0;
            long posA = empA.getPositionId() != null ? empA.getPositionId() : 0;
            long posB = empB.getPositionId() != null ? empB.getPositionId() : 0;
            return Long.compare(posB, posA); // 높은 직급(큰 positionId) 우선
        });

        model.addAttribute("results", results);

        // 9. 등급 분포 통계
        Map<String, Long> gradeDistribution = results.stream()
                .filter(r -> r.gradeCode() != null)
                .collect(Collectors.groupingBy(EvaluationResultDTO::gradeCode, Collectors.counting()));
        model.addAttribute("gradeDistribution", gradeDistribution);
        model.addAttribute("totalCount", results.size());

        return "eval/result/list";
    }

    /**
     * 사전 조회된 데이터를 이용하여 특정 유형의 점수를 0~100 범위로 환산합니다.
     * DB 호출 없이 인메모리에서 계산합니다.
     */
    private Integer calculateTypeScoreInMemory(List<EvaluationElementDTO> allElements,
                                                Map<Long, Evaluation> evalMap,
                                                String typeCode) {
        List<EvaluationElementDTO> typeElements = allElements.stream()
                .filter(e -> typeCode.equals(e.elementTypeCode()))
                .collect(Collectors.toList());

        if (typeElements.isEmpty()) return null;

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (EvaluationElementDTO elem : typeElements) {
            Evaluation eval = evalMap.get(elem.elementId());
            if (eval == null || eval.getScore() == null) continue;

            BigDecimal score = BigDecimal.valueOf(eval.getScore());
            BigDecimal maxScore = elem.maxScore();
            BigDecimal weight = elem.weight();

            if (maxScore.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal normalizedScore = score.divide(maxScore, 10, RoundingMode.HALF_UP).multiply(weight);
            weightedSum = weightedSum.add(normalizedScore);
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) return null;

        return weightedSum.divide(totalWeight, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private List<EvaluationElementDTO> getElementsWithFallback(Long periodId, Long deptId) {
        if (deptId != null) {
            List<EvaluationElementDTO> elements = elementService.getElementsByPeriodId(periodId, deptId);
            if (!elements.isEmpty()) return elements;
        }
        return elementService.getElementsByPeriodId(periodId, null);
    }
}
