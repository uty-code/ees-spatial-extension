package com.ees.eval.controller;


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
    private final EvaluatorMappingMapper evaluatorMappingMapper;

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

        List<EvaluationPeriodDTO> periods = periodService.getInProgressPeriods();
        model.addAttribute("periods", periods);

        EvaluationPeriodDTO selectedPeriod = null;
        if (periodId != null) {
            selectedPeriod = periodService.getPeriodById(periodId);
        } else if (!periods.isEmpty()) {
            selectedPeriod = periods.stream()
                    .filter(p -> "IN_PROGRESS".equals(p.statusCode()))
                    .findFirst()
                    .orElse(periods.isEmpty() ? null : periods.get(0));
        }

        if (selectedPeriod != null) {
            model.addAttribute("selectedPeriod", selectedPeriod);

            // 내가 평가해야 할 매핑 중 'SUBORDINATE' 관계인 것만 필터링 (다면평가)
            List<EvaluatorMappingDTO> myTasks = mappingService.getMyEvaluationTasks(selectedPeriod.periodId(), empId);
            List<EvaluatorMappingDTO> multiTasks = myTasks.stream()
                    .filter(m -> "SUBORDINATE".equals(m.relationTypeCode()))
                    .collect(Collectors.toList());

            model.addAttribute("tasks", multiTasks);

            // ========== [최적화] 데이터 일괄 조회 ==========
            if (!multiTasks.isEmpty()) {
                // (A) 모든 매핑 ID 수집 및 평가 데이터 일괄 조회
                java.util.List<Long> allMappingIds = multiTasks.stream()
                        .map(EvaluatorMappingDTO::mappingId)
                        .collect(Collectors.toList());

                // (B) 피평가자 ID 수집 및 SELF 매핑 일괄 조회
                java.util.List<Long> evaluateeIds = multiTasks.stream()
                        .map(EvaluatorMappingDTO::evaluateeId)
                        .distinct()
                        .collect(Collectors.toList());

                // SELF 매핑 추출 (findByEvaluateeIds로 한 번에 조회)
                java.util.Map<Long, Long> selfMappingIdByEvaluatee = new java.util.HashMap<>();
                java.util.List<Long> selfMappingIds = new java.util.ArrayList<>();
                // 피평가자별 전체 매핑 그룹 (잠금 체크용 재사용)
                java.util.Map<Long, java.util.List<com.ees.eval.domain.EvaluatorMapping>> allMappingsByEvaluatee = new java.util.HashMap<>();
                java.util.List<Long> downstreamMappingIds = new java.util.ArrayList<>();
                if (!evaluateeIds.isEmpty()) {
                    java.util.List<com.ees.eval.domain.EvaluatorMapping> allRelatedMappings =
                            evaluatorMappingMapper.findByEvaluateeIds(selectedPeriod.periodId(), evaluateeIds);
                    allMappingsByEvaluatee = allRelatedMappings.stream()
                            .collect(Collectors.groupingBy(com.ees.eval.domain.EvaluatorMapping::getEvaluateeId));
                    for (com.ees.eval.domain.EvaluatorMapping m : allRelatedMappings) {
                        if ("SELF".equals(m.getRelationTypeCode()) && "n".equals(m.getIsDeleted())) {
                            selfMappingIdByEvaluatee.put(m.getEvaluateeId(), m.getMappingId());
                            selfMappingIds.add(m.getMappingId());
                        }
                        if ("MANAGER".equals(m.getRelationTypeCode()) || "EXECUTIVE".equals(m.getRelationTypeCode())) {
                            if (!allMappingIds.contains(m.getMappingId())) {
                                downstreamMappingIds.add(m.getMappingId());
                            }
                        }
                    }
                }

                // 전체 관련 매핑 ID (내 것 + SELF + 다운스트림)
                java.util.List<Long> allRelMappingIds = new java.util.ArrayList<>(allMappingIds);
                allRelMappingIds.addAll(selfMappingIds);
                allRelMappingIds.addAll(downstreamMappingIds);

                // (C) 평가 데이터 일괄 조회
                java.util.Map<Long, java.util.List<Evaluation>> evalGroupMap = new java.util.HashMap<>();
                if (!allRelMappingIds.isEmpty()) {
                    evalGroupMap = evaluationMapper.findByMappingIds(allRelMappingIds).stream()
                            .collect(Collectors.groupingBy(Evaluation::getMappingId));
                }

                // ========== 제출 여부 및 자가평가 확인 (메모리에서) ==========
                java.util.Map<Long, Boolean> submittedMap = new java.util.HashMap<>();
                java.util.Map<Long, Boolean> evaluateeSelfSubmittedMap = new java.util.HashMap<>();
                for (EvaluatorMappingDTO task : multiTasks) {
                    // 본인의 제출 여부
                    java.util.List<Evaluation> evals = evalGroupMap.getOrDefault(task.mappingId(), java.util.Collections.emptyList());
                    boolean isSubmitted = evals.stream()
                            .anyMatch(e -> "SUBMITTED".equals(e.getConfirmStatusCode()));
                    submittedMap.put(task.mappingId(), isSubmitted);

                    // 피평가자(부서장)의 자가평가 제출 여부
                    Long selfMappingId = selfMappingIdByEvaluatee.get(task.evaluateeId());
                    boolean isSelfSubmitted = false;
                    if (selfMappingId != null) {
                        java.util.List<Evaluation> selfEvals = evalGroupMap.getOrDefault(selfMappingId, java.util.Collections.emptyList());
                        isSelfSubmitted = selfEvals.stream()
                                .anyMatch(e -> "SUBMITTED".equals(e.getConfirmStatusCode()));
                    }
                    evaluateeSelfSubmittedMap.put(task.mappingId(), isSelfSubmitted);
                }
                model.addAttribute("submittedMap", submittedMap);
                model.addAttribute("evaluateeSelfSubmittedMap", evaluateeSelfSubmittedMap);

                // 역순 진행 방지 (사전 조회 데이터 활용 — 추가 DB 호출 없음)
                java.util.Map<Long, Boolean> lockMap = mappingService.checkEvaluationLockBulk(
                        allMappingIds, allMappingsByEvaluatee, evalGroupMap);
                model.addAttribute("lockMap", lockMap);
            } else {
                model.addAttribute("submittedMap", java.util.Collections.emptyMap());
                model.addAttribute("evaluateeSelfSubmittedMap", java.util.Collections.emptyMap());
                model.addAttribute("lockMap", java.util.Collections.emptyMap());
            }

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

        boolean submitted = savedMap.values().stream()
                .anyMatch(e -> "SUBMITTED".equals(e.getConfirmStatusCode()));
        model.addAttribute("submitted", submitted);

        // 역순 진행 방지 (상위 평가자가 제출했는지 확인)
        java.util.Map<String, Object> lockInfo = mappingService.checkEvaluationLock(mappingId);
        model.addAttribute("isLocked", lockInfo.get("isLocked"));
        model.addAttribute("lockedBy", lockInfo.get("lockedBy"));

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
        
        // 역순 진행 방지 검증
        java.util.Map<String, Object> lockInfo = mappingService.checkEvaluationLock(mappingId);
        if ((Boolean) lockInfo.get("isLocked")) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                lockInfo.get("lockedBy") + "가 평가를 완료하여 더 이상 수정할 수 없습니다.");
            return "redirect:/eval/multi-dimensional/form?mappingId=" + mappingId;
        }

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

        EvaluatorMappingDTO submitMapping = mappingService.getMappingById(mappingId);
        redirectAttributes.addFlashAttribute("successMessage", "다면평가가 제출되었습니다.");
        return "redirect:/eval/multi-dimensional?periodId=" + submitMapping.periodId();
    }
}
