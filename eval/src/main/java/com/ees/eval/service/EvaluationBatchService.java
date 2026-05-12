package com.ees.eval.service;

/**
 * 평가 자동 집계 및 배치 작업을 담당하는 서비스 인터페이스입니다.
 */
public interface EvaluationBatchService {
    
    /**
     * 현재 진행 중인 차수의 모든 피평가자에 대해 시스템 자동 평가(운영 KPI)를 집계하고 저장합니다.
     */
    void runAutomatedEvaluations();
}
