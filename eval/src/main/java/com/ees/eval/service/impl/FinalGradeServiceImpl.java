package com.ees.eval.service.impl;

import com.ees.eval.domain.Employee;
import com.ees.eval.domain.Evaluation;
import com.ees.eval.domain.EvaluatorMapping;
import com.ees.eval.dto.EvaluationElementDTO;
import com.ees.eval.dto.FinalGradeTaskDTO;
import com.ees.eval.mapper.EmployeeMapper;
import com.ees.eval.mapper.EvaluationMapper;
import com.ees.eval.mapper.EvaluatorMappingMapper;
import com.ees.eval.service.EvaluationElementService;
import com.ees.eval.service.EvaluationTypeWeightService;
import com.ees.eval.service.FinalGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FinalGradeService의 구현체입니다.
 * 벌크 조회 및 메모리 매핑을 통해 성능을 최적화합니다.
 */
@Service
@RequiredArgsConstructor
public class FinalGradeServiceImpl implements FinalGradeService {

    private final EvaluatorMappingMapper mappingMapper;
    private final EvaluationMapper evaluationMapper;
    private final EmployeeMapper employeeMapper;
    private final EvaluationElementService elementService;
    private final EvaluationTypeWeightService typeWeightService;

    @Override
    @Transactional(readOnly = true)
    public List<FinalGradeTaskDTO> getFinalGradeTasks(Long periodId, Long executiveEmpId) {
        // 1. 임원의 평가 대상 목록(EXECUTIVE 매핑) 조회
        List<EvaluatorMapping> teamTasks = mappingMapper.findByEvaluatorId(periodId, executiveEmpId);
        if (teamTasks.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 피평가자 ID 목록 추출 및 사원 정보 벌크 조회
        List<Long> evaluateeIds = teamTasks.stream()
                .map(EvaluatorMapping::getEvaluateeId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, Employee> employeeMap = employeeMapper.findByIds(evaluateeIds).stream()
                .collect(Collectors.toMap(Employee::getEmpId, e -> e));

        // 3. 피평가자들의 모든 매핑 정보(SELF 포함) 벌크 조회
        List<EvaluatorMapping> allMappingsForEvaluatees = mappingMapper.findByEvaluateeIds(periodId, evaluateeIds);
        
        // 피평가자별 SELF 매핑 ID 맵 구성
        Map<Long, Long> selfMappingIdMap = allMappingsForEvaluatees.stream()
                .filter(m -> "SELF".equals(m.getRelationTypeCode()))
                .collect(Collectors.toMap(EvaluatorMapping::getEvaluateeId, EvaluatorMapping::getMappingId));

        // 4. 모든 관련 매핑 ID에 대한 평가 결과 벌크 조회
        List<Long> allRelatedMappingIds = allMappingsForEvaluatees.stream()
                .map(EvaluatorMapping::getMappingId)
                .collect(Collectors.toList());
        
        List<Evaluation> allEvals = evaluationMapper.findByMappingIds(allRelatedMappingIds);
        
        // 매핑 ID별 평가 결과 그룹화
        Map<Long, List<Evaluation>> evalGroupMap = allEvals.stream()
                .collect(Collectors.groupingBy(Evaluation::getMappingId));

        // 5. 부서별 가중치 유효성 및 평가 요소 정보 캐싱
        Map<Long, Boolean> weightValidCache = new HashMap<>();
        // 전사 공통 요소 (deptId = null)
        List<EvaluationElementDTO> globalElements = elementService.getElementsByPeriodId(periodId, null);

        // 6. 결과 DTO 조립
        return teamTasks.stream().map(task -> {
            Employee evaluatee = employeeMap.get(task.getEvaluateeId());
            Long deptId = (evaluatee != null) ? evaluatee.getDeptId() : null;

            // 가중치 유효성 체크 (캐시 활용)
            boolean weightValid = weightValidCache.computeIfAbsent(deptId, 
                id -> typeWeightService.isWeightSumValid(periodId, id, "STAFF"));

            // 본인 평가(EXECUTIVE) 완료 여부
            List<EvaluationElementDTO> elements = getElementsWithFallback(periodId, deptId, globalElements);
            List<Evaluation> myEvals = evalGroupMap.getOrDefault(task.getMappingId(), Collections.emptyList());
            boolean allSubmitted = isAllSubmitted(elements, myEvals);

            // 자가평가(SELF) 제출 여부
            Long selfMappingId = selfMappingIdMap.get(task.getEvaluateeId());
            boolean selfSubmitted = selfMappingId != null && evalGroupMap.containsKey(selfMappingId);

            return FinalGradeTaskDTO.builder()
                    .mappingId(task.getMappingId())
                    .evaluateeId(task.getEvaluateeId())
                    .evaluateeName(task.getEvaluateeName())
                    .deptName(task.getDeptName())
                    .allSubmitted(allSubmitted)
                    .selfSubmitted(selfSubmitted)
                    .weightValid(weightValid)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<EvaluationElementDTO> getElementsWithFallback(Long periodId, Long deptId, List<EvaluationElementDTO> globalElements) {
        if (deptId == null) return globalElements;
        List<EvaluationElementDTO> elements = elementService.getElementsByPeriodId(periodId, deptId);
        return elements.isEmpty() ? globalElements : elements;
    }

    private boolean isAllSubmitted(List<EvaluationElementDTO> elements, List<Evaluation> evaluations) {
        if (elements.isEmpty()) return false;
        Set<Long> evaluatedElementIds = evaluations.stream()
                .map(Evaluation::getElementId)
                .collect(Collectors.toSet());
        return elements.stream().allMatch(e -> evaluatedElementIds.contains(e.elementId()));
    }
}
