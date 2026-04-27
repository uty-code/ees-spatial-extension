package com.ees.eval.service;

import com.ees.eval.dto.EmployeeDTO;
import com.ees.eval.dto.DepartmentDTO;
import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.dto.EvaluatorMappingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EvaluatorMappingService의 통합 테스트 클래스입니다.
 * 중복 체크, 자기평가 검증, 일괄 매핑, 내 평가 목록/나를 평가하는 사람 조회를 검증합니다.
 * 시드 데이터와의 격리를 위해 테스트 전용 부서를 생성하여 사용합니다.
 */
@SpringBootTest
@Transactional
class EvaluatorMappingServiceTest extends com.ees.eval.support.AbstractMssqlTest {

    @Autowired
    private EvaluatorMappingService mappingService;

    @Autowired
    private EvaluationPeriodService periodService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    /** 테스트용 차수 ID */
    private Long testPeriodId;

    /** 테스트 전용 부서 ID (시드 데이터와 격리) */
    private Long testDeptId;

    /** 테스트용 사원 ID (A: ROLE_MANAGER, B: ROLE_USER, C: ROLE_USER, D: ROLE_EXECUTIVE, E: ROLE_ADMIN) */
    private Long empIdA;
    private Long empIdB;
    private Long empIdC;
    private Long empIdD;
    private Long empIdE;
    private String nameA;
    private String nameB;
    private String nameC;
    private String nameD;
    private String nameE;
    private Long otherDeptId;
    private Long otherLeaderId;

    /**
     * 각 테스트 전에 전용 부서 + 차수 1건 + 사원 3명을 미리 등록합니다.
     * 시드 데이터(eval-data.sql)와의 간섭을 방지하기 위해 별도의 부서를 생성합니다.
     */
    @BeforeEach
    void setUp() {
        // 부서 생성 (testDeptId - 최상위 부서)
        DepartmentDTO dept = departmentService.createDepartment(
                DepartmentDTO.builder().deptName("테스트본부").parentDeptId(null).build());
        testDeptId = dept.deptId();

        // 다른 부서 생성 (otherDeptId)
        DepartmentDTO dept2 = departmentService.createDepartment(
                DepartmentDTO.builder().deptName("기타부서").parentDeptId(null).build());
        otherDeptId = dept2.deptId();

        // 평가 기간 생성
        EvaluationPeriodDTO period = periodService.createPeriod(
                EvaluationPeriodDTO.builder()
                        .periodYear(2026).periodName("상반기 평가")
                        .startDate(LocalDate.of(2026, 1, 1))
                        .endDate(LocalDate.of(2026, 6, 30)).build());
        testPeriodId = period.periodId();

        // 사원 A (testDeptId의 리더로 지정될 예정)
        EmployeeDTO empA = employeeService.registerEmployee(
                EmployeeDTO.builder()
                        .deptId(testDeptId).positionId(4L) // 팀장
                        .password("passA!")
                        .name("김팀장").email("leaderA@ees.com")
                        .hireDate(LocalDate.of(2020, 1, 1)).build(),
                List.of(2L)); // ROLE_MANAGER
        empIdA = empA.empId();
        nameA = empA.name();

        // 사원 B (testDeptId 소속 일반 사원)
        EmployeeDTO empB = employeeService.registerEmployee(
                EmployeeDTO.builder()
                        .deptId(testDeptId).positionId(1L)
                        .password("passB!")
                        .name("이팀원").email("memberB@ees.com")
                        .hireDate(LocalDate.of(2022, 1, 1)).build(),
                List.of(1L)); // ROLE_USER
        empIdB = empB.empId();
        nameB = empB.name();

        // 사원 C (testDeptId 소속 일반 사원)
        EmployeeDTO empC = employeeService.registerEmployee(
                EmployeeDTO.builder()
                        .deptId(testDeptId).positionId(1L)
                        .password("passC!")
                        .name("박사원").email("memberC@ees.com")
                        .hireDate(LocalDate.of(2025, 1, 1)).build(),
                List.of(1L)); // ROLE_USER
        empIdC = empC.empId();
        nameC = empC.name();

        // 사원 D (testDeptId 소속 임원)
        EmployeeDTO empD = employeeService.registerEmployee(
                EmployeeDTO.builder()
                        .deptId(testDeptId).positionId(6L) // 이사
                        .password("passD!")
                        .name("윤임원").email("execD@ees.com")
                        .hireDate(LocalDate.of(2018, 1, 1)).build(),
                List.of(3L)); // ROLE_EXECUTIVE
        empIdD = empD.empId();
        nameD = empD.name();

        // 다른 부서의 리더 (otherLeaderId)
        EmployeeDTO otherLeader = employeeService.registerEmployee(
                EmployeeDTO.builder()
                        .deptId(otherDeptId).positionId(4L)
                        .password("passOther!")
                        .name("타부서장").email("other@ees.com")
                        .hireDate(LocalDate.of(2019, 1, 1)).build(),
                List.of(2L));
        otherLeaderId = otherLeader.empId();

        // 사원 E (ROLE_ADMIN 권한 - 시스템 관리자)
        EmployeeDTO empE = employeeService.registerEmployee(
                EmployeeDTO.builder()
                        .deptId(testDeptId).positionId(1L)
                        .password("passE!")
                        .name("관리자E").email("adminE@ees.com")
                        .hireDate(LocalDate.of(2019, 1, 1)).build(),
                List.of(4L)); // ROLE_ADMIN
        empIdE = empE.empId();
        nameE = empE.name();

        // 부서장 지정 (testDeptId의 리더를 empIdA로 설정)
        departmentService.assignLeader(testDeptId, empIdA);
    }

    /**
     * 단건 매핑 생성 및 조회를 검증합니다.
     */
    @Test
    @DisplayName("단건 매핑 생성 - 부서장 평가 매핑")
    void createSingleMappingTest() {
        // when: A(팀장)가 B(사원)의 부서장 평가자로 매핑
        EvaluatorMappingDTO created = mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId)
                        .evaluateeId(empIdB)
                        .evaluatorId(empIdA)
                        .relationTypeCode("MANAGER")
                        .build());

        // then: 생성 직후 반환된 DTO가 아닌, ID로 재조회하여 JOIN된 정보(이름) 검증
        EvaluatorMappingDTO mapping = mappingService.getMappingById(created.mappingId());
        
        assertThat(mapping.mappingId()).isNotNull();
        assertThat(mapping.evaluateeId()).isEqualTo(empIdB);
        assertThat(mapping.evaluateeName()).isEqualTo(nameB);
        assertThat(mapping.evaluatorId()).isEqualTo(empIdA);
        assertThat(mapping.evaluatorName()).isEqualTo(nameA);
        assertThat(mapping.relationTypeCode()).isEqualTo("MANAGER");
    }

    /**
     * 동일 차수에서 동일 관계가 중복되면 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("중복 체크 - 동일 관계 중복 생성 차단")
    void duplicateMappingBlockedTest() {
        // given: 최초 매핑 성공
        mappingService.createMapping(EvaluatorMappingDTO.builder()
                .periodId(testPeriodId).evaluateeId(empIdB)
                .evaluatorId(empIdA).relationTypeCode("MANAGER").build());

        // then: 동일 매핑 재시도 → 예외
        assertThatThrownBy(() -> mappingService.createMapping(EvaluatorMappingDTO.builder()
                .periodId(testPeriodId).evaluateeId(empIdB)
                .evaluatorId(empIdA).relationTypeCode("MANAGER").build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 존재");
    }

    /**
     * SELF가 아닌 유형(SUPERIOR 등)으로 자기 자신을 매핑하면 예외가 발생하고,
     * SELF 유형은 본인 매핑이 허용되는지 검증합니다.
     */
    @Test
    @DisplayName("자기평가 검증 - SELF 허용, 기타 유형 본인 매핑 차단")
    void selfMappingValidationTest() {
        // SELF 유형으로 본인 매핑 → 허용 (일반 사원인 empIdB)
        EvaluatorMappingDTO selfMapping = mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId).evaluateeId(empIdB)
                        .evaluatorId(empIdB).relationTypeCode("SELF").build());
        assertThat(selfMapping.mappingId()).isNotNull();
        assertThat(selfMapping.relationTypeCode()).isEqualTo("SELF");

        // MANAGER 유형으로 본인 매핑 → 차단
        assertThatThrownBy(() -> mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId).evaluateeId(empIdB)
                        .evaluatorId(empIdB).relationTypeCode("MANAGER").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자기 자신은 '본인' 관계 유형으로만 매핑이 가능합니다");
    }

    /**
     * 자동 매핑 생성 시 임원(ROLE_EXECUTIVE)과 시스템 관리자(ROLE_ADMIN) 역할의 사원은
     * SELF(자기평가) 매핑이 생성되지 않는지 검증합니다.
     * 부서장(ROLE_MANAGER)과 일반 사원(ROLE_USER)은 SELF 매핑이 정상 생성되어야 합니다.
     */
    @Test
    @DisplayName("자동 매핑 - 임원/시스템관리자 SELF 제외, 부서장/일반사원 허용")
    void autoGenerateMappings_excludesExecutiveAndAdminFromSelfTest() {
        // when: 테스트 전용 부서 대상 자동 매핑 생성 (시드 데이터와 격리)
        mappingService.autoGenerateMappings(testPeriodId, testDeptId, null);

        // then: empIdA(ROLE_MANAGER, 부서장)의 매핑 목록에 SELF가 있어야 함
        List<EvaluatorMappingDTO> aEvaluators = mappingService.getMyEvaluators(testPeriodId, empIdA);
        boolean aHasSelf = aEvaluators.stream()
                .anyMatch(m -> "SELF".equals(m.relationTypeCode()));
        assertThat(aHasSelf)
                .as("ROLE_MANAGER 역할의 부서장(%d)에게도 SELF 매핑이 생성되어야 합니다", empIdA)
                .isTrue();

        // then: empIdB(ROLE_USER)의 매핑 목록에도 SELF가 있어야 함
        List<EvaluatorMappingDTO> bEvaluators = mappingService.getMyEvaluators(testPeriodId, empIdB);
        boolean bHasSelf = bEvaluators.stream()
                .anyMatch(m -> "SELF".equals(m.relationTypeCode()));
        assertThat(bHasSelf)
                .as("ROLE_USER 역할의 사원(%d)에게는 SELF 매핑이 생성되어야 합니다", empIdB)
                .isTrue();

        // then: empIdD(ROLE_EXECUTIVE, 임원)의 매핑 목록에 SELF가 없어야 함
        List<EvaluatorMappingDTO> dEvaluators = mappingService.getMyEvaluators(testPeriodId, empIdD);
        boolean dHasSelf = dEvaluators.stream()
                .anyMatch(m -> "SELF".equals(m.relationTypeCode()));
        assertThat(dHasSelf)
                .as("ROLE_EXECUTIVE 역할의 임원(%d)에게는 SELF 매핑이 생성되지 않아야 합니다", empIdD)
                .isFalse();

        // then: empIdE(ROLE_ADMIN, 시스템 관리자)의 매핑 목록에 SELF가 없어야 함
        List<EvaluatorMappingDTO> eEvaluators = mappingService.getMyEvaluators(testPeriodId, empIdE);
        boolean eHasSelf = eEvaluators.stream()
                .anyMatch(m -> "SELF".equals(m.relationTypeCode()));
        assertThat(eHasSelf)
                .as("ROLE_ADMIN 역할의 시스템 관리자(%d)에게는 SELF 매핑이 생성되지 않아야 합니다", empIdE)
                .isFalse();
    }

    /**
     * 타인을 '본인' 평가 유형으로 매핑하려 할 때 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("수동 매핑 - 타인을 '본인' 유형으로 지정 시 차단")
    void manualSelfMapping_WithDifferentUser_Fail() {
        assertThatThrownBy(() -> mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId).evaluateeId(empIdB)
                        .evaluatorId(empIdC).relationTypeCode("SELF").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'본인' 관계 유형은 피평가자와 평가자가 동일해야 합니다");
    }

    /**
     * 타 부서의 부서장이나 일반 사원을 '부서장(MANAGER)' 평가 유형으로 매핑하려 할 때 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("수동 매핑 - 정식 부서장이 아닌 사원을 '부서장' 유형으로 지정 시 차단")
    void manualManagerMapping_IncorrectLeader_Fail() {
        // then: empIdC(일반 사원)를 empIdB의 MANAGER로 지정 시도 -> 실패
        assertThatThrownBy(() -> mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId).evaluateeId(empIdB)
                        .evaluatorId(empIdC).relationTypeCode("MANAGER").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 부서의 공식 부서장(Leader)만");
    }

    /**
     * 최상위 부서 소속이라도 ROLE_EXECUTIVE 권한이 없는 사원을 '임원(EXECUTIVE)' 유형으로 매핑하려 할 때 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("수동 매핑 - ROLE_EXECUTIVE 권한 없는 사원을 '임원' 유형으로 지정 시 차단")
    void manualExecutiveMapping_WithoutRole_Fail() {
        // then: empIdB(일반 사원, 최상위 부서 소속)를 empIdC의 EXECUTIVE로 지정 시도 -> 실패
        assertThatThrownBy(() -> mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId).evaluateeId(empIdC)
                        .evaluatorId(empIdB).relationTypeCode("EXECUTIVE").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("임원(ROLE_EXECUTIVE) 권한이 없습니다");
    }

    /**
     * 일괄 매핑: 한 명의 피평가자에게 여러 평가자를 동시 매핑합니다.
     */
    @Test
    @DisplayName("일괄 매핑 - 부서장 및 다면 평가자 대량 생성")
    void bulkMappingTest() {
        // when 1: A(팀장)가 B, C를 일괄 MANAGER로 매핑
        // (실제 로직상 B의 매니저는 A, C의 매니저도 A여야 함 - 현재 테스트 셋팅상 둘 다 testDeptId이므로 가능)
        mappingService.createBulkMappings(testPeriodId, empIdB, List.of(empIdA), "MANAGER");
        mappingService.createBulkMappings(testPeriodId, empIdC, List.of(empIdA), "MANAGER");

        // when 2: B, C가 A(팀장)를 일괄 SUBORDINATE로 매핑 (상향 평가)
        mappingService.createBulkMappings(testPeriodId, empIdA, List.of(empIdB, empIdC), "SUBORDINATE");

        // then: A가 해야 할 평가 → 2건 (B, C의 MANAGER)
        assertThat(mappingService.getMyEvaluationTasks(testPeriodId, empIdA)).hasSize(2);
        
        // then: A를 평가하는 사람 → 2건 (B, C의 SUBORDINATE)
        assertThat(mappingService.getMyEvaluators(testPeriodId, empIdA)).hasSize(2);
    }

    /**
     * '내가 수행해야 할 평가 목록'과 '나를 평가하는 사람 목록'을 정확히 조회합니다.
     */
    @Test
    @DisplayName("평가 목록 조회 - 내 평가 과제 / 나를 평가하는 사람")
    void evaluationTaskAndEvaluatorListTest() {
        // given 1: A(팀장)가 B(사원)를 평가 (MANAGER)
        mappingService.createMapping(EvaluatorMappingDTO.builder()
                .periodId(testPeriodId).evaluateeId(empIdB)
                .evaluatorId(empIdA).relationTypeCode("MANAGER").build());

        // given 2: C(사원)가 A(팀장)를 평가 (SUBORDINATE - 상향 평가)
        mappingService.createMapping(EvaluatorMappingDTO.builder()
                .periodId(testPeriodId).evaluateeId(empIdA)
                .evaluatorId(empIdC).relationTypeCode("SUBORDINATE").build());

        // when: A의 '내가 해야 할 평가 목록' → 1건 (B를 평가하는 MANAGER)
        List<EvaluatorMappingDTO> aTasks = mappingService.getMyEvaluationTasks(testPeriodId, empIdA);
        assertThat(aTasks).hasSize(1);
        assertThat(aTasks.get(0).evaluateeId()).isEqualTo(empIdB);

        // when: A의 '나를 평가하는 사람 목록' → 1건 (C가 평가하는 SUBORDINATE)
        List<EvaluatorMappingDTO> aEvaluators = mappingService.getMyEvaluators(testPeriodId, empIdA);
        assertThat(aEvaluators).hasSize(1);
        assertThat(aEvaluators.get(0).evaluatorId()).isEqualTo(empIdC);
    }
    @Test
    @DisplayName("매핑 조회 - 피평가자 기준으로만 검색 결과가 나오는지 검증")
    void searchMappings_EvaluateeOnly_Success() {
        // given: A(피평가자) - B(평가자) 매핑 생성 (SUBORDINATE는 누구나 가능)
        mappingService.createMapping(EvaluatorMappingDTO.builder()
                .periodId(testPeriodId).evaluateeId(empIdA)
                .evaluatorId(empIdB).relationTypeCode("SUBORDINATE").build());

        // 1. 피평가자 사번(empIdA)으로 검색 시 -> 결과 나옴
        String keywordA = String.valueOf(empIdA);
        List<EvaluatorMappingDTO> resultsA = mappingService.getMappingsByPeriodIdAndDeptId(testPeriodId, null, keywordA);
        assertThat(resultsA).isNotEmpty();
        assertThat(resultsA.get(0).evaluateeId()).isEqualTo(empIdA);

        // 2. 평가자 사번(empIdB)으로 검색 시 -> 결과 없음 (평가자 기준 검색은 제외)
        String keywordB = String.valueOf(empIdB);
        List<EvaluatorMappingDTO> resultsB = mappingService.getMappingsByPeriodIdAndDeptId(testPeriodId, null, keywordB);
        assertThat(resultsB).isEmpty();
    }
    /**
     * 다면 평가자(SUBORDINATE) 매핑 시 피평가자가 부서장이 아닐 경우 실패하는지 검증합니다.
     */
    @Test
    @DisplayName("수동 매핑 - 일반 사원을 '다면 평가(SUBORDINATE)' 대상으로 지정 시 차단")
    void manualSubordinateMapping_ToGeneralEmployee_Fail() {
        // then: empIdB(일반 사원)를 피평가자로, empIdC를 다면 평가자로 지정 시도 -> 실패
        assertThatThrownBy(() -> mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId).evaluateeId(empIdB)
                        .evaluatorId(empIdC).relationTypeCode("SUBORDINATE").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("부서장만 다면 평가(상향 평가)의 대상이 될 수 있습니다. 일반 사원은 다면 평가 대상이 아닙니다.");
    }

    /**
     * 타 부서 사원을 다면 평가자로 매핑 시도 시 실패하는지 검증합니다.
     */
    @Test
    @DisplayName("수동 매핑 - 타 부서 사원을 '다면 평가자'로 지정 시 차단")
    void manualSubordinateMapping_DifferentDept_Fail() {
        // then: empIdA(본인 팀장)의 다면 평가자로 otherLeaderId(타 부서장) 지정 시도 -> 실패
        assertThatThrownBy(() -> mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId).evaluateeId(empIdA)
                        .evaluatorId(otherLeaderId).relationTypeCode("SUBORDINATE").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소속 부서가 일치하지 않습니다");
    }

    /**
     * 타 부서의 부서장을 MANAGER로 매핑 시도 시 실패하는지 검증합니다.
     */
    @Test
    @DisplayName("수동 매핑 - 타 부서 부서장을 '1차 평가자(MANAGER)'로 지정 시 차단")
    void manualManagerMapping_DifferentDept_Fail() {
        // then: empIdB(본인 팀원)의 1차 평가자로 otherLeaderId(타 부서장) 지정 시도 -> 실패
        assertThatThrownBy(() -> mappingService.createMapping(
                EvaluatorMappingDTO.builder()
                        .periodId(testPeriodId).evaluateeId(empIdB)
                        .evaluatorId(otherLeaderId).relationTypeCode("MANAGER").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소속 부서가 일치하지 않습니다");
    }
}
