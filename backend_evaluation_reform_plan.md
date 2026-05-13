# Backend Evaluation Logic Reform Plan (v2.0)
## Spatial BI & Operational Data Integration

This document outlines the technical reform plan to transition the existing EES evaluation process into a data-driven system integrated with GIS-based operational metrics.

---

## 1. Core Architectural Changes

### 1.1 Data Model Extensions
*   **Relation Type Addition**: Add `OPERATIONAL` (or `SYSTEM`) to `relation_type_code` in `evaluator_mappings_51`. This allows the system to act as a "virtual evaluator" that provides objective data scores.
*   **Evaluation Correction Factors**: 지역별 영업 난이도 계수(`Regional Difficulty Coefficient`)를 정의하여 단순 절대 성과가 아닌 '노력 대비 성과'를 측정합니다.
    *   **Coefficient Criteria**: 지점 밀집도(`Branch Density`), 상권 공실률(`Vacancy Rate`), 경쟁 강도(`Competitor Density`) 등을 공간 쿼리로 분석하여 산출.
    *   **Range**: `RegionalDifficultyCoefficient`는 **0.95 ~ 1.05** 범위 내에서만 계산되어 급격한 점수 변동을 방지합니다.
    *   **Formula**: $CorrectedScore = BaseScore \times RegionalDifficultyCoefficient$
    *   **Abuse Prevention**: 보정 지표는 최종 점수 대비 최대 **±5점** 범위 내에서만 제한적으로 적용되며, 보조 지표로 활용됩니다.

### 1.2 Evaluation Pipeline Reform
The evaluation flow will be processed in the following order:
1.  **Pre-Calculation**: System aggregates branch performance for each manager.
2.  **Qualitative Input**: Managers/Employees perform qualitative evaluations (Leadership, etc.).
3.  **Cross-Analysis**: System compares qualitative scores with operational performance.
4.  **Executive BI Review**: Final approval supported by Spatial BI dashboards.

---

## 2. 점수 체계 및 가중치 (Evaluation Weights)

데이터의 객관성과 관리자의 통찰력을 균형 있게 반영하기 위해 다음과 같은 가중치를 권장합니다.

| 영역 | 비중 | 설명 |
| :--- | :--- | :--- |
| **Operational KPI** | 50% | 담당 지점 매출 성장률, 클레임 등 데이터 기반 정량 점수 |
| **Manager Evaluation** | 30% | 상급자의 리더십 및 협업 능력 정성 평가 |
| **Self Evaluation** | 10% | 본인 수행 과제 및 성찰 점수 |
| **Spatial Correction** | 10% | 지역 난이도 계수가 반영된 보정 점수 |
| **Total** | **100%** | |

$$FinalScore = (OpKPI \times 0.5) + (MgrEval \times 0.3) + (SelfEval \times 0.1) + (SpatialCorr \times 0.1)$$

*   **SpatialCorr 정의**: `RegionalDifficultyCoefficient`가 반영된 최종 보정 점수(0~100)이며, 지점 환경에 따른 노력도를 독립적인 항목으로 평가에 반영합니다.
*   **Precision Policy**: 모든 최종 점수 및 가중 합산 결과는 **소수점 둘째 자리에서 반올림**하여 저장합니다.

---

## 2. Implementation Plan by Stage

### Stage 1: Automated Operational Indexing
**Goal**: Automatically convert store data into evaluation scores.

*   **Logic**:
    *   Fetch all branches managed by an employee from `branch_managers_51`.
    *   Aggregate `revenue_growth`, `claim_count`, and `composite_score` from `branch_performance_51` for the current period.
    *   **Normalization**: 수집된 지표는 **Min-Max Scaling** 기법을 사용하여 0~100 범위로 정규화합니다.
*   **Batch Processing**:
    *   **수행 시점**: 매 분기 종료 직후(1, 4, 7, 10월 초) Spring Scheduler 기반 자동 집계 수행.
    *   **중복 방지**: 동일 연도/분기에 대한 자동 집계가 중복 실행되지 않도록 `evaluation_periods_51` 테이블의 상태값을 검증하는 Idempotency 로직을 적용합니다. (멀티 인스턴스 환경 고려 시 **Distributed Lock** 적용 권장)
    *   **Failure Recovery**: 배치 실패 시 최대 **3회 자동 재시도**를 수행하며, 최종 실패 시 상태를 `FAILED`로 기록하고 알림을 발생시킵니다.
    *   **KPI Snapshotting**: 분기 종료 및 집계 시점의 원천 데이터를 **Snapshot**으로 저장하여, 향후 과거 지점 실적 데이터가 수정되더라도 평가 당시의 점수가 보존되도록 설계합니다.
    *   **대상**: 활성화된 모든 매장 관리자 및 지점 성과 데이터.
*   **Service**: `AutomatedEvaluationService`
    *   `calculateBranchKPIs(Long empId, Integer year, Integer quarter)`
    *   `populateSystemEvaluation(Integer year, Integer quarter, Long evaluateeId)`: 특정 사원의 분기 실적을 `OPERATIONAL` 타입으로 자동 적재.

### Stage 2: 관리자 정성 평가 (Qualitative Input)
**Goal**: 기존 EES 역할 및 입력 주체 명확화.

*   **Role Boundary**:
    *   **MANAGER / SELF**: 평가자(사람)가 UI를 통해 직접 입력하는 정성/주관 점수.
    *   **OPERATIONAL**: 평가 기간 종료 시 시스템 스케줄러에 의해 자동 산출되는 정량 점수.
*   **Note**: No major logic changes, but the UI will show the "Automatic Score" from Stage 1 as a reference.

### Stage 3: Spatial BI & Cross-Analysis (The Core)
**Goal**: Verify evaluations against regional context.

*   **Anomaly Detection**:
    *   **Over/Under-evaluated Detection**: 데이터 기반 정량 점수와 관리자 정성 점수의 차이가 **15점 이상**일 경우 리스크 신호 발생.
    *   **Critical Gap**: 상위 5% 관리자 점수를 받았으나, 지점 성과가 하위 20%인 경우 '정밀 검토' 대상으로 분류.
*   **Regional Correction (Spatial Factor)**:
    *   Use `location` data to calculate branch density and competition levels.
    *   **Spatial Query Example**: `WHERE branch.location.STDistance(@target) < 500`
    *   **Rationale**: 500m 반경은 실제 고객의 **도보 상권 범위**를 기준으로 설정하여 지역 내 경쟁 밀집도를 정확히 측정합니다.
    *   Adjust the "Automatic Score" using a **Correction Index**.
    *   *Example*: A manager in a "Declining Commercial District" receives a +5 point bonus to compensate for environmental factors.

### Stage 4: Executive Data-Driven Approval
**Goal**: Provide executives with visual evidence for final grading.

*   **API Enhancements**:
    *   `getExecutiveReviewContext(Long evaluateeId)`: Returns a structured data object including:
        *   **Performance Trend**: 4분기 연속 성과 추세 그래프 데이터.
        *   **Risk Level**: 폐점 위험도 및 운영 안정성 지수.
        *   **Evaluation Gap**: 데이터 기반 정량 점수와 관리자 정성 점수의 차이를 **조회 시점에 동적으로 계산**하여 반환. (15점 이상 시 리스크 신호)
        *   **Regional Difficulty**: 해당 지점이 위치한 지역의 난이도 계수 및 공간 분석 결과.
*   **Status Enum Definition**:
    | Status | 의미 | 비고 |
    | :--- | :--- | :--- |
    | **NORMAL** | 정상 | 편차 10점 미만 |
    | **WARNING** | 편차 주의 | 편차 10점 ~ 15점 미만 |
    | **ANOMALY_DETECTED** | 이상 징후 | 편차 15점 이상 (정밀 검토 필요) |
*   **Data Persistence Policy**:
    *   **Score Freeze**: 임원 최종 승인 완료 시점에 `FinalScore`를 `final_grades_51` 테이블에 확정 저장(Freeze)합니다.
    *   **Re-approval**: 확정 이후 점수를 수정해야 할 경우, `evaluation_audit_logs_51` 테이블에 `evaluator_id`, `modified_at`, `previous_score`, `new_score`를 기록하고 재승인 프로세스를 수행합니다.
*   **Performance Optimization**:
    *   **Redis Cache**: 조회 부하가 큰 Executive Review API에 한정하여 Redis 캐시를 적용합니다. (최대 **10분 TTL** 설정)
    *   **Cache Invalidation**: 평가 데이터 갱신, 관리자 평가 제출, 또는 임원 승인 완료 시점에 해당 사원의 캐시를 즉시 무효화(Evict)합니다.
*   **Security & Access Control**:
    *   시스템 전반에 **RBAC(Role-Based Access Control)**를 적용하며, 주요 권한은 `ROLE_EXECUTIVE`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`로 구분합니다.
    *   본 API는 **임원(ROLE_EXECUTIVE)** 권한을 가진 사용자만 접근 가능하며, 요청 시 세션/토큰 기반 권한 검증을 수행합니다.
*   **Response Example**:
    ```json
    {
      "evaluateeId": 1001,
      "finalScore": 82.4,
      "riskLevel": "MID",
      "evaluationGap": 18.2,
      "regionalDifficulty": 1.08,
      "status": "ANOMALY_DETECTED"
    }
    ```
*   **Visualizations**: Integrate Kakao Map overlays showing "Performance vs. Evaluation" color-coded markers.

---

## 3. Anticipated Benefits

1.  **Enhanced Objectivity**: 50% of the evaluation is based on actual revenue and claim data.
2.  **Strategic GIS Utilization**: GIS is no longer just for visualization; it directly impacts employee performance grading through environmental correction.
3.  **Risk Management**: Executives can instantly spot "soft" evaluations where store performance is failing but manager ratings are high.

---

## 4. Technical Stack Impact
*   **Backend**: Spring Boot + MyBatis (Extend `EvaluationResultService`)
*   **Database**: SQL Server Spatial (Utilize `GEOGRAPHY` types and `STDistance` for density checks)
*   **Frontend**: Thymeleaf + Kakao Map API (Develop "Executive Evaluation Insight" component)
