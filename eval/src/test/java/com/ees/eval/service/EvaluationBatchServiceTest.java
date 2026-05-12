package com.ees.eval.service;

import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.mapper.EvaluatorMappingMapper;
import com.ees.eval.service.impl.EvaluationBatchServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationBatchServiceTest {

    @Mock
    private EvaluationPeriodService evaluationPeriodService;

    @Mock
    private EvaluatorMappingMapper evaluatorMappingMapper;

    @Mock
    private AutomatedEvaluationService automatedEvaluationService;

    @Mock
    private com.ees.eval.mapper.BatchJobHistoryMapper batchJobHistoryMapper;

    @InjectMocks
    private EvaluationBatchServiceImpl evaluationBatchService;

    @Test
    @DisplayName("자동 평가 배치 - 진행 중인 차수의 모든 피평가자에 대해 집계 로직이 호출된다")
    void runAutomatedEvaluations_ShouldProcessAllEvaluatees() {
        // given
        Long periodId = 10L;
        EvaluationPeriodDTO mockPeriod = EvaluationPeriodDTO.builder()
                .periodId(periodId)
                .periodYear(2024)
                .periodName("2024년 상반기 평가")
                .build();
        
        given(evaluationPeriodService.getInProgressPeriods()).willReturn(List.of(mockPeriod));
        given(evaluatorMappingMapper.findDistinctEvaluateesByPeriodId(periodId)).willReturn(List.of(1001L, 1002L));

        // when
        evaluationBatchService.runAutomatedEvaluations();

        // then
        verify(automatedEvaluationService, times(1)).populateSystemEvaluation(eq(2024), eq(1), eq(1001L));
        verify(automatedEvaluationService, times(1)).populateSystemEvaluation(eq(2024), eq(1), eq(1002L));
    }
}
