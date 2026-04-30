package com.ees.eval.controller;

import com.ees.eval.domain.Employee;
import com.ees.eval.domain.Evaluation;
import com.ees.eval.dto.EvaluationElementDTO;
import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.dto.EvaluatorMappingDTO;
import com.ees.eval.mapper.EmployeeMapper;
import com.ees.eval.mapper.EvaluationMapper;
import com.ees.eval.service.EvaluationElementService;
import com.ees.eval.service.EvaluationPeriodService;
import com.ees.eval.service.EvaluationTypeWeightService;
import com.ees.eval.service.EvaluatorMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 다면평가(Multi-dimensional Evaluation) 컨트롤러
 * 부서원(Subordinate)이 부서장(Leader)을 평가하는 기능을 담당합니다.
 */
@Slf4j
@Controller
@RequestMapping("/eval/multi-dimensional")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MultiDimensionalEvaluationController {

    private final EvaluationPeriodService periodService;
    private final EvaluatorMappingService mappingService;
    private final EvaluationElementService elementService;
    private final EvaluationTypeWeightService typeWeightService;
    private final EvaluationMapper evaluationMapper;
    private final EmployeeMapper employeeMapper;

    /**
     * 피평가자의 부서에 맞는 평가요소를 조회합니다.
     */
    private List<EvaluationElementDTO> getElementsWithFallback(Long periodId, Long deptId) {
        if (deptId != null) {
            return elementService.getElementsByPeriodId(periodId, deptId);
        }
        return elementService.getElementsByPeriodId(periodId, null);
    }

    /**
     * 다면평가 대상 목록 페이지
     */
    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) Long periodId,
                       @AuthenticationPrincipal UserDetails userDetails) {

        Long empId = Long.parseLong(userDetails.getUsername());
        model.addAttribute("activeMenu", "multi-dimensional-eval");

        List<EvaluationPeriodDTO> periods = periodService.getAllPeriods();
        model.addAttribute("periods", periods);

        EvaluationPeriodDTO selectedPeriod = null;
        if (periodId != null) {
            selectedPeriod = periodService.getPeriodById(periodId);
        } else if (!periods.isEmpty()) {
            selectedPeriod = periods.stream()
                    .filter(p -> "ACTIVE".equals(p.statusCode()))
                    .findFirst()
                    .orElse(periods.get(0));
        }

        if (selectedPeriod != null) {
            model.addAttribute("selectedPeriod", selectedPeriod);

            // 내가 평가해야 할 매핑 중 'SUBORDINATE' 관계인 것만 필터링 (다면평가)
            List<EvaluatorMappingDTO> myTasks = mappingService.getMyEvaluationTasks(selectedPeriod.periodId(), empId);
            List<EvaluatorMappingDTO> multiTasks = myTasks.stream()
                    .filter(m -> "SUBORDINATE".equals(m.relationTypeCode()))
                    .collect(Collectors.toList());

            model.addAttribute("tasks", multiTasks);

            // 제출 여부 확인 Map 및 피평가자 자가평가 여부 확인
            java.util.Map<Long, Boolean> submittedMap = new java.util.HashMap<>();
            java.util.Map<Long, Boolean> evaluateeSelfSubmittedMap = new java.util.HashMap<>();
            for (EvaluatorMappingDTO task : multiTasks) {
                // 본인의 제출 여부
                List<Evaluation> evals = evaluationMapper.findByMappingId(task.mappingId());
                boolean isSubmitted = evals.stream()
                        .anyMatch(e -> "SUBMITTED".equals(e.getConfirmStatusCode()));
                submittedMap.put(task.mappingId(), isSubmitted);

                // 피평가자(부서장)의 자가평가 제출 여부
                List<EvaluatorMappingDTO> evaluateeTasks = mappingService.getMyEvaluationTasks(selectedPeriod.periodId(), task.evaluateeId());
                boolean isSelfSubmitted = evaluateeTasks.stream()
                        .filter(m -> "SELF".equals(m.relationTypeCode()))
                        .findFirst()
                        .map(selfMapping -> evaluationMapper.findByMappingId(selfMapping.mappingId()).stream()
                                .anyMatch(e -> "SUBMITTED".equals(e.getConfirmStatusCode())))
                        .orElse(false);
                evaluateeSelfSubmittedMap.put(task.mappingId(), isSelfSubmitted);
            }
            model.addAttribute("submittedMap", submittedMap);
            model.addAttribute("evaluateeSelfSubmittedMap", evaluateeSelfSubmittedMap);

            if ("PLANNED".equals(selectedPeriod.statusCode())) {
                model.addAttribute("infoMessage", "현재 평가 시작 전입니다. 정해진 평가 기간에만 작성이 가능합니다.");
            }
        }

        return "eval/multi-dimensional/list";
    }

    /**
     * 다면평가 입력 폼
     */
    @GetMapping("/form")
    public String getForm(@RequestParam Long mappingId,
                          Model model,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {

        EvaluatorMappingDTO mapping = mappingService.getMappingById(mappingId);

        // SUBORDINATE 관계인지 재검증
        if (!"SUBORDINATE".equals(mapping.relationTypeCode())) {
            redirectAttributes.addFlashAttribute("errorMessage", "다면평가 대상이 아닙니다.");
            return "redirect:/eval/multi-dimensional";
        }

        // 평가 시작 전 접근 차단
        EvaluationPeriodDTO period = periodService.getPeriodById(mapping.periodId());
        if ("PLANNED".equals(period.statusCode())) {
            redirectAttributes.addFlashAttribute("errorMessage", "평가 시작 전입니다.");
            return "redirect:/eval/multi-dimensional?periodId=" + mapping.periodId();
        }

        // 가중치 검증 (피평가자가 부서장이므로 LEADER 기준)
        Employee evaluatee = employeeMapper.findById(mapping.evaluateeId()).orElse(null);
        Long evaluateeDeptId = (evaluatee != null) ? evaluatee.getDeptId() : null;
        if (!typeWeightService.isWeightSumValid(mapping.periodId(), evaluateeDeptId, "LEADER")) {
            redirectAttributes.addFlashAttribute("errorMessage", "유형별 가중치 설정(LEADER)이 올바르지 않습니다.");
            return "redirect:/eval/multi-dimensional?periodId=" + mapping.periodId();
        }

        model.addAttribute("mapping", mapping);

        // 다면평가 요소(MULTI_DIMENSIONAL)만 필터링
        List<EvaluationElementDTO> allElements = getElementsWithFallback(mapping.periodId(), evaluateeDeptId);
        List<EvaluationElementDTO> elements = allElements.stream()
                .filter(e -> "MULTI_DIMENSIONAL".equals(e.elementTypeCode()))
                .collect(Collectors.toList());

        model.addAttribute("elements", elements);
        model.addAttribute("mappingId", mappingId);

        // 기존 저장 데이터
        java.util.Map<Long, Evaluation> savedMap = evaluationMapper.findByMappingId(mappingId)
                .stream()
                .collect(Collectors.toMap(Evaluation::getElementId, e -> e, (a, b) -> a));
        model.addAttribute("savedMap", savedMap);

        // 제출 여부
        boolean submitted = savedMap.values().stream()
                .anyMatch(e -> "SUBMITTED".equals(e.getConfirmStatusCode()));
        model.addAttribute("submitted", submitted);

        // 피평가자(부서장)의 자가평가 데이터 조회 (다면평가 참고용)
        List<EvaluatorMappingDTO> evaluateeTasks = mappingService.getMyEvaluationTasks(mapping.periodId(), mapping.evaluateeId());
        EvaluatorMappingDTO evaluateeSelfTask = evaluateeTasks.stream()
                .filter(m -> "SELF".equals(m.relationTypeCode()))
                .findFirst()
                .orElse(null);

        if (evaluateeSelfTask != null) {
            List<Evaluation> evaluateeEvals = evaluationMapper.findByMappingId(evaluateeSelfTask.mappingId());
            java.util.Map<Long, Evaluation> evaluateeSelfEvalMap = evaluateeEvals.stream()
                    .collect(Collectors.toMap(Evaluation::getElementId, e -> e, (a, b) -> a));
            
            boolean isEvaluateeSelfSubmitted = evaluateeEvals.stream()
                    .anyMatch(e -> "SUBMITTED".equals(e.getConfirmStatusCode()));
            
            model.addAttribute("evaluateeSelfEvalMap", evaluateeSelfEvalMap);
            model.addAttribute("isEvaluateeSelfSubmitted", isEvaluateeSelfSubmitted);
        } else {
            model.addAttribute("isEvaluateeSelfSubmitted", false);
        }

        return "eval/multi-dimensional/form";
    }

    /**
     * 다면평가 제출
     */
    @PostMapping("/submit")
    public String submitForm(@RequestParam Long mappingId,
                             @RequestParam java.util.Map<String, String> params,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        Long empId = Long.parseLong(userDetails.getUsername());
        
        // 데이터 저장 로직 (PerformanceEvaluationController와 유사)
        java.util.Set<Long> elementIds = new java.util.HashSet<>();
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
                } catch (Exception e) {}
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

        redirectAttributes.addFlashAttribute("successMessage", "다면평가가 제출되었습니다.");
        return "redirect:/eval/multi-dimensional/form?mappingId=" + mappingId;
    }
}
