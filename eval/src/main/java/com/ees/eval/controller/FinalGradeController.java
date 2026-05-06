package com.ees.eval.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.ees.eval.domain.Employee;
import com.ees.eval.domain.Evaluation;
import com.ees.eval.dto.EvaluationElementDTO;
import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.dto.EvaluatorMappingDTO;
import com.ees.eval.mapper.EmployeeMapper;
import com.ees.eval.mapper.EvaluationMapper;
import com.ees.eval.mapper.EvaluatorMappingMapper;
import com.ees.eval.service.EvaluationElementService;
import com.ees.eval.service.EvaluationPeriodService;
import com.ees.eval.service.EvaluationTypeWeightService;
import com.ees.eval.service.EvaluatorMappingService;
import com.ees.eval.service.FinalGradeService;
import com.ees.eval.dto.FinalGradeTaskDTO;
import com.ees.eval.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/eval/final-grade")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
public class FinalGradeController {

    private final EvaluationPeriodService periodService;
    private final EvaluatorMappingService mappingService;
    private final EvaluationElementService elementService;
    private final EvaluationTypeWeightService typeWeightService;
    private final EvaluationMapper evaluationMapper;
    private final EvaluatorMappingMapper evaluatorMappingMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final FinalGradeService finalGradeService;

    private List<EvaluationElementDTO> getElementsWithFallback(Long periodId, Long deptId) {
        if (deptId != null) {
            return elementService.getElementsByPeriodId(periodId, deptId);
        }
        return elementService.getElementsByPeriodId(periodId, null);
    }

    @GetMapping
    public String list(@RequestParam(name = "periodId", required = false) Long periodId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {
        
        model.addAttribute("activeMenu", "final-grade");
        Long executiveEmpId = Long.parseLong(userDetails.getUsername());
        
        // 1. 차수 목록 및 현재 선택된 차수 정보 (진행 중인 차수만 표시)
        List<EvaluationPeriodDTO> periods = periodService.getInProgressPeriods();
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

        if (selectedPeriod != null) {
            if ("PLANNED".equals(selectedPeriod.statusCode())) {
                model.addAttribute("infoMessage", "현재 평가 시작 전입니다.");
            } else {
                // 2. [최적화] FinalGradeService를 통해 벌크 조회 및 상태 플래그 계산
                List<FinalGradeTaskDTO> tasks = finalGradeService.getFinalGradeTasks(selectedPeriod.periodId(), executiveEmpId);
                model.addAttribute("tasks", tasks);

                // 3. 기존 뷰 템플릿과의 호환성을 위해 맵 구성
                Map<Long, Boolean> teamSubmittedMap = new HashMap<>();
                Map<Long, Boolean> selfSubmittedMap = new HashMap<>();
                Map<Long, Boolean> weightValidMap = new HashMap<>();

                for (FinalGradeTaskDTO task : tasks) {
                    teamSubmittedMap.put(task.mappingId(), task.allSubmitted());
                    selfSubmittedMap.put(task.mappingId(), task.selfSubmitted());
                    weightValidMap.put(task.mappingId(), task.weightValid());
                }

                model.addAttribute("teamSubmittedMap", teamSubmittedMap);
                model.addAttribute("evaluateeSelfSubmittedMap", selfSubmittedMap);
                model.addAttribute("teamWeightValidMap", weightValidMap);
            }
        }

        return "eval/final-grade/list";
    }

    @GetMapping("/form")
    public String getForm(@RequestParam Long mappingId,
                          Model model,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {

        EvaluatorMappingDTO mapping = mappingService.getMappingById(mappingId);

        EvaluationPeriodDTO period = periodService.getPeriodById(mapping.periodId());
        if ("PLANNED".equals(period.statusCode())) {
            redirectAttributes.addFlashAttribute("errorMessage", "평가 시작 전입니다.");
            return "redirect:/eval/final-grade?periodId=" + mapping.periodId();
        }

        Employee evaluatee = employeeMapper.findById(mapping.evaluateeId()).orElse(null);
        Long evaluateeDeptId = (evaluatee != null) ? evaluatee.getDeptId() : null;

        boolean isLeader = departmentMapper.countDepartmentsByLeaderId(mapping.evaluateeId()) > 0;
        String targetRoleCode = isLeader ? "LEADER" : "STAFF";

        if (!typeWeightService.isWeightSumValid(mapping.periodId(), evaluateeDeptId, targetRoleCode)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "유형별 가중치 합계가 100%가 아닙니다. 관리자에게 가중치 설정을 요청하세요.");
            return "redirect:/eval/final-grade?periodId=" + mapping.periodId();
        }

        model.addAttribute("mapping", mapping);
        model.addAttribute("activeMenu", "final-grade");
        model.addAttribute("mappingId", mappingId);
        model.addAttribute("isLeader", isLeader);

        List<EvaluationElementDTO> allElements = getElementsWithFallback(mapping.periodId(), evaluateeDeptId);
        
        List<EvaluationElementDTO> performanceElements = isLeader ? List.of() : allElements.stream()
            .filter(e -> "PERFORMANCE".equals(e.elementTypeCode()))
            .toList();
        List<EvaluationElementDTO> competencyElements = allElements.stream()
            .filter(e -> isLeader ? "MULTI_DIMENSIONAL".equals(e.elementTypeCode()) : "COMPETENCY".equals(e.elementTypeCode()))
            .toList();

        model.addAttribute("performanceElements", performanceElements);
        model.addAttribute("competencyElements", competencyElements);

        // 기존 작성 내용
        Map<Long, Evaluation> savedMap = evaluationMapper
            .findByMappingId(mappingId)
            .stream()
            .collect(Collectors.toMap(Evaluation::getElementId, e -> e, (a, b) -> a));
        model.addAttribute("savedMap", savedMap);

        // 자가평가 내용 참조
        Map<Long, Evaluation> selfEvalMap = evaluatorMappingMapper
            .findByEvaluateeId(mapping.periodId(), mapping.evaluateeId())
            .stream()
            .filter(m -> "SELF".equals(m.getRelationTypeCode()))
            .findFirst()
            .map(selfMapping -> evaluationMapper.findByMappingId(selfMapping.getMappingId())
                .stream()
                .collect(Collectors.toMap(Evaluation::getElementId, e -> e, (a, b) -> a)))
            .orElse(java.util.Collections.emptyMap());
        model.addAttribute("selfEvalMap", selfEvalMap);

        // 1차 평가자(MANAGER) 내용 참조
        Map<Long, Evaluation> managerEvalMap = evaluatorMappingMapper
            .findByEvaluateeId(mapping.periodId(), mapping.evaluateeId())
            .stream()
            .filter(m -> "MANAGER".equals(m.getRelationTypeCode()))
            .findFirst()
            .map(managerMapping -> evaluationMapper.findByMappingId(managerMapping.getMappingId())
                .stream()
                .collect(Collectors.toMap(Evaluation::getElementId, e -> e, (a, b) -> a)))
            .orElse(java.util.Collections.emptyMap());
        model.addAttribute("managerEvalMap", managerEvalMap);

        log.info("[FinalGrade] evaluateeId={}, selfEvalMap keys={}, managerEvalMap keys={}",
                 mapping.evaluateeId(), selfEvalMap.keySet(), managerEvalMap.keySet());

        // 아직 작성 내역이 없으면 MANAGER 내용으로 기본 세팅
        if (savedMap.isEmpty() && !managerEvalMap.isEmpty()) {
            savedMap.putAll(managerEvalMap);
        }
        
        // 모든 항목이 제출되었는지 확인
        Set<Long> targetIds = allElements.stream()
            .filter(e -> isLeader ? "MULTI_DIMENSIONAL".equals(e.elementTypeCode()) : 
                         ("PERFORMANCE".equals(e.elementTypeCode()) || "COMPETENCY".equals(e.elementTypeCode())))
            .map(EvaluationElementDTO::elementId)
            .collect(Collectors.toSet());
        
        boolean submitted = !targetIds.isEmpty() && savedMap.entrySet().stream()
            .filter(entry -> targetIds.contains(entry.getKey()))
            .allMatch(entry -> "SUBMITTED".equals(entry.getValue().getConfirmStatusCode()));
        model.addAttribute("submitted", submitted);

        return "eval/final-grade/wizard";
    }

    @PostMapping("/submit")
    public String submitForm(@RequestParam Long mappingId,
                             @RequestParam Map<String, String> params,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        Long empId = Long.parseLong(userDetails.getUsername());
        
        EvaluatorMappingDTO submitMapping = mappingService.getMappingById(mappingId);
        Employee submitEvaluatee = employeeMapper.findById(submitMapping.evaluateeId()).orElse(null);
        Long submitDeptId = (submitEvaluatee != null) ? submitEvaluatee.getDeptId() : null;

        boolean isLeader = departmentMapper.countDepartmentsByLeaderId(submitMapping.evaluateeId()) > 0;
        String targetRoleCode = isLeader ? "LEADER" : "STAFF";

        if (!typeWeightService.isWeightSumValid(submitMapping.periodId(), submitDeptId, targetRoleCode)) {
            redirectAttributes.addFlashAttribute("errorMessage", "유형별 가중치 합계가 올바르지 않습니다.");
            return "redirect:/eval/final-grade/form?mappingId=" + mappingId;
        }

        Set<Long> elementIds = new java.util.HashSet<>();
        params.keySet().forEach(key -> {
            if (key.startsWith("comment_") || key.startsWith("score_")) {
                try {
                    elementIds.add(Long.parseLong(key.substring(key.indexOf("_") + 1)));
                } catch (Exception ignore) {}
            }
        });

        for (Long elementId : elementIds) {
            String comment = params.get("comment_" + elementId);
            String scoreStr = params.get("score_" + elementId);

            Integer score = null;
            if (scoreStr != null && !scoreStr.trim().isEmpty()) {
                try {
                    score = Integer.valueOf(scoreStr.trim());
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "잘못된 점수 형식입니다.");
                    return "redirect:/eval/final-grade/form?mappingId=" + mappingId;
                }
            }

            final Integer finalScore = score;
            final String finalComment = (comment != null) ? comment.trim() : "";

            evaluationMapper.findByMappingIdAndElementId(mappingId, elementId)
                .ifPresentOrElse(
                    existing -> {
                        existing.setReason(finalComment);
                        existing.setScore(finalScore);
                        existing.setConfirmStatusCode("SUBMITTED");
                        existing.preUpdate();
                        evaluationMapper.update(existing);
                    },
                    () -> {
                        Evaluation eval = Evaluation.builder()
                            .mappingId(mappingId)
                            .elementId(elementId)
                            .confirmStatusCode("SUBMITTED")
                            .build();
                        eval.setReason(finalComment);
                        eval.setScore(finalScore);
                        eval.prePersist();
                        eval.setCreatedBy(empId);
                        eval.setUpdatedBy(empId);
                        evaluationMapper.insert(eval);
                    }
                );
        }

        redirectAttributes.addFlashAttribute("successMessage", "평가가 성공적으로 제출되었습니다.");
        return "redirect:/eval/final-grade/form?mappingId=" + mappingId;
    }
}
