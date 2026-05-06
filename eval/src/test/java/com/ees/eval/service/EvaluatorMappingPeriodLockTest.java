package com.ees.eval.service;

import com.ees.eval.dto.DepartmentDTO;
import com.ees.eval.dto.EmployeeDTO;
import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.dto.EvaluatorMappingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 평가 진행 상태(IN_PROGRESS 이상)에서 평가자 매핑 CUD 차단을 검증하는 테스트입니다.
 * TDD 방식으로 작성: 실패하는 테스트를 먼저 작성한 후 구현 코드를 추가합니다.
 *
 * <p>검증 대상 비즈니스 규칙:</p>
 * <ul>
 *     <li>PLANNED 상태: 매핑 생성/수정/삭제 허용</li>
 *     <li>IN_PROGRESS/COMPLETED/CLOSED 상태: 매핑 생성/수정/삭제 차단 (IllegalStateException)</li>
 * </ul>
 */
@SpringBootTest
@Transactional
@DisplayName("평가 진행 상태에서 평가자 매핑 수정 차단 테스트")
class EvaluatorMappingPeriodLockTest extends com.ees.eval.support.AbstractMssqlTest {

    @Autowired
    private EvaluatorMappingService mappingService;

    @Autowired
    private EvaluationPeriodService periodService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private com.ees.eval.mapper.EvaluationPeriodMapper periodMapper;

    /** 테스트용 차수 ID (초기 상태: PLANNED) */
    private Long plannedPeriodId;

    /** 테스트 전용 부서 ID */
    private Long testDeptId;

    /** 사원 A: 부서장(ROLE_MANAGER), 사원 B: 일반 사원(ROLE_USER) */
    private Long empIdA;
    private Long empIdB;

    /**
     * 각 테스트 전에 전용 부서, PLANNED 상태 차수, 사원 2명을 생성합니다.
     */
    @BeforeEach
    void setUp() {
        // 테스트 전용 부서 생성
        DepartmentDTO dept = departmentService.createDepartment(
                DepartmentDTO.builder().deptName("잠금테스트부서").parentDeptId(null).build());
        testDeptId = dept.deptId();

        // PLANNED 상태의 평가 차수 생성
        EvaluationPeriodDTO period = periodService.createPeriod(
                EvaluationPeriodDTO.builder()
                        .periodYear(2026).periodName("잠금 테스트 차수")
                        .startDate(LocalDate.of(2026, 1, 1))
                        .endDate(LocalDate.of(2026, 6, 30)).build());
        plannedPeriodId = period.periodId();

        // 사원 A: 부서장
        EmployeeDTO empA = employeeService.registerEmployee(
                EmployeeDTO.builder()
                        .deptId(testDeptId).positionId(4L)
                        .password("testA!")
                        .name("잠금팀장").email("lock-leader@ees.com")
                        .hireDate(LocalDate.of(2020, 1, 1)).build(),
                List.of(2L)); // ROLE_MANAGER
        empIdA = empA.empId();

        // 사원 B: 일반 사원
        EmployeeDTO empB = employeeService.registerEmployee(
                EmployeeDTO.builder()
                        .deptId(testDeptId).positionId(1L)
                        .password("testB!")
                        .name("잠금팀원").email("lock-member@ees.com")
                        .hireDate(LocalDate.of(2022, 1, 1)).build(),
                List.of(1L)); // ROLE_USER
        empIdB = empB.empId();

        // 부서장 지정
        departmentService.assignLeader(testDeptId, empIdA);
    }

    // ========================================================================
    // PLANNED 상태: 정상 동작 검증
    // ========================================================================

    @Nested
    @DisplayName("PLANNED 상태에서의 정상 동작")
    class PlannedStateTests {

        @Test
        @DisplayName("should_성공_when_PLANNED_상태에서_매핑생성")
        void should_성공_when_PLANNED_상태에서_매핑생성() {
            // when: PLANNED 상태에서 매핑 생성
            EvaluatorMappingDTO created = mappingService.createMapping(
                    EvaluatorMappingDTO.builder()
                            .periodId(plannedPeriodId)
                            .evaluateeId(empIdB)
                            .evaluatorId(empIdA)
                            .relationTypeCode("MANAGER")
                            .build());

            // then: 정상 생성
            assertThat(created.mappingId()).isNotNull();
            assertThat(created.evaluateeId()).isEqualTo(empIdB);
        }

        @Test
        @DisplayName("should_성공_when_PLANNED_상태에서_자동생성")
        void should_성공_when_PLANNED_상태에서_자동생성() {
            // when: PLANNED 상태에서 자동 생성
            int count = mappingService.autoGenerateMappings(plannedPeriodId, testDeptId, null);

            // then: 1건 이상 생성
            assertThat(count).isGreaterThan(0);
        }
    }

    // ========================================================================
    // IN_PROGRESS 상태: 차단 검증 (핵심 테스트)
    // ========================================================================

    @Nested
    @DisplayName("IN_PROGRESS 상태에서의 CUD 차단")
    class InProgressStateTests {

        /**
         * 차수를 IN_PROGRESS 상태로 직접 변경합니다.
         * transitionStatus()는 가중치 설정 등 사전 조건 검증이 포함되어 있으므로,
         * 상태 잠금 로직만 순수하게 테스트하기 위해 Mapper를 통해 직접 상태를 변경합니다.
         */
        private void transitionToInProgress() {
            com.ees.eval.domain.EvaluationPeriod period = periodMapper.findById(plannedPeriodId)
                    .orElseThrow();
            period.setStatusCode("IN_PROGRESS");
            periodMapper.update(period);
        }

        @Test
        @DisplayName("should_throw_when_평가진행중_매핑생성")
        void should_throw_when_평가진행중_매핑생성() {
            // given: 차수를 IN_PROGRESS로 전이
            transitionToInProgress();

            // when & then: 매핑 생성 시도 → IllegalStateException
            assertThatThrownBy(() -> mappingService.createMapping(
                    EvaluatorMappingDTO.builder()
                            .periodId(plannedPeriodId)
                            .evaluateeId(empIdB)
                            .evaluatorId(empIdA)
                            .relationTypeCode("MANAGER")
                            .build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("매핑을 변경할 수 없습니다");
        }

        @Test
        @DisplayName("should_throw_when_평가진행중_일괄매핑생성")
        void should_throw_when_평가진행중_일괄매핑생성() {
            // given: 차수를 IN_PROGRESS로 전이
            transitionToInProgress();

            // when & then: 일괄 매핑 생성 시도 → IllegalStateException
            assertThatThrownBy(() -> mappingService.createBulkMappings(
                    plannedPeriodId, empIdB, List.of(empIdA), "MANAGER"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("매핑을 변경할 수 없습니다");
        }

        @Test
        @DisplayName("should_throw_when_평가진행중_자동생성")
        void should_throw_when_평가진행중_자동생성() {
            // given: 차수를 IN_PROGRESS로 전이
            transitionToInProgress();

            // when & then: 자동 생성 시도 → IllegalStateException
            assertThatThrownBy(() -> mappingService.autoGenerateMappings(
                    plannedPeriodId, testDeptId, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("매핑을 변경할 수 없습니다");
        }

        @Test
        @DisplayName("should_throw_when_평가진행중_매핑삭제")
        void should_throw_when_평가진행중_매핑삭제() {
            // given: PLANNED 상태에서 매핑을 미리 생성
            EvaluatorMappingDTO created = mappingService.createMapping(
                    EvaluatorMappingDTO.builder()
                            .periodId(plannedPeriodId)
                            .evaluateeId(empIdB)
                            .evaluatorId(empIdA)
                            .relationTypeCode("MANAGER")
                            .build());
            Long mappingId = created.mappingId();

            // given: 차수를 IN_PROGRESS로 전이
            transitionToInProgress();

            // when & then: 삭제 시도 → IllegalStateException
            assertThatThrownBy(() -> mappingService.deleteMapping(mappingId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("매핑을 변경할 수 없습니다");
        }

        @Test
        @DisplayName("should_throw_when_평가진행중_매핑수정")
        void should_throw_when_평가진행중_매핑수정() {
            // given: PLANNED 상태에서 매핑을 미리 생성
            EvaluatorMappingDTO created = mappingService.createMapping(
                    EvaluatorMappingDTO.builder()
                            .periodId(plannedPeriodId)
                            .evaluateeId(empIdB)
                            .evaluatorId(empIdA)
                            .relationTypeCode("MANAGER")
                            .build());
            Long mappingId = created.mappingId();

            // given: 차수를 IN_PROGRESS로 전이
            transitionToInProgress();

            // when & then: 평가자 변경 시도 → IllegalStateException
            assertThatThrownBy(() -> mappingService.updateMapping(mappingId, empIdB))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("매핑을 변경할 수 없습니다");
        }

        @Test
        @DisplayName("should_throw_when_평가진행중_일괄초기화")
        void should_throw_when_평가진행중_일괄초기화() {
            // given: 차수를 IN_PROGRESS로 전이
            transitionToInProgress();

            // when & then: 일괄 초기화 시도 → IllegalStateException
            assertThatThrownBy(() -> mappingService.initializeMappingsByDept(
                    plannedPeriodId, testDeptId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("매핑을 변경할 수 없습니다");
        }
    }
}
