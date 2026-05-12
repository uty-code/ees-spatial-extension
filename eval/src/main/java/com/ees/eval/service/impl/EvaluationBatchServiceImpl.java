package com.ees.eval.service.impl;

import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.mapper.EvaluatorMappingMapper;
import com.ees.eval.service.AutomatedEvaluationService;
import com.ees.eval.service.EvaluationBatchService;
import com.ees.eval.service.EvaluationPeriodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 평가 자동 집계 배치 서비스 구현체입니다.
 * 정기적으로 또는 수동으로 시스템 자동 평가 점수를 적재합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationBatchServiceImpl implements EvaluationBatchService {

    private final EvaluationPeriodService evaluationPeriodService;
    private final EvaluatorMappingMapper evaluatorMappingMapper;
    private final AutomatedEvaluationService automatedEvaluationService;
    private final com.ees.eval.mapper.BatchJobHistoryMapper batchJobHistoryMapper;

    /**
     * 매일 새벽 2시에 자동 평가 점수 집계 (예시 스케줄)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Override
    @Transactional
    public void runAutomatedEvaluations() {
        log.info("자동 평가 점수 집계 배치 시작");
        
        List<EvaluationPeriodDTO> inProgressPeriods = evaluationPeriodService.getInProgressPeriods();
        if (inProgressPeriods.isEmpty()) {
            log.info("진행 중인 평가 차수가 없어 배치를 종료합니다.");
            return;
        }

        for (EvaluationPeriodDTO period : inProgressPeriods) {
            // 1. History READY 생성
            com.ees.eval.domain.BatchJobHistory history = com.ees.eval.domain.BatchJobHistory.builder()
                    .jobName("OPERATIONAL_KPI_AGGREGATION")
                    .periodId(period.periodId())
                    .status("RUNNING")
                    .startTime(java.time.LocalDateTime.now())
                    .build();
            batchJobHistoryMapper.insert(history);

            log.info("차수 처리 중: {} (ID: {})", period.periodName(), period.periodId());
            
            try {
                // 해당 차수의 모든 피평가자 조회
                List<Long> evaluateeIds = evaluatorMappingMapper.findDistinctEvaluateesByPeriodId(period.periodId());
                int successCount = 0;
                int failCount = 0;

                for (Long evaluateeId : evaluateeIds) {
                    try {
                        int year = period.periodYear();
                        int quarter = 1; 
                        automatedEvaluationService.populateSystemEvaluation(year, quarter, evaluateeId);
                        successCount++;
                    } catch (Exception e) {
                        log.error("피평가자(ID: {}) 자동 평가 적재 중 오류 발생: {}", evaluateeId, e.getMessage());
                        failCount++;
                    }
                }

                // 2. History SUCCESS 업데이트
                history.setStatus("SUCCESS");
                history.setEndTime(java.time.LocalDateTime.now());
                history.setResultMessage(String.format("성공: %d건, 실패: %d건", successCount, failCount));
                batchJobHistoryMapper.update(history);

            } catch (Exception e) {
                log.error("차수(ID: {}) 처리 중 치명적 오류 발생: {}", period.periodId(), e.getMessage());
                history.setStatus("FAILED");
                history.setEndTime(java.time.LocalDateTime.now());
                history.setResultMessage("치명적 오류: " + e.getMessage());
                batchJobHistoryMapper.update(history);
            }
        }
        
        log.info("자동 평가 점수 집계 배치 완료");
    }
}
