package com.ees.eval.mapper;

import com.ees.eval.dto.BranchRiskDTO;
import com.ees.eval.support.AbstractMssqlTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("BranchRiskMapper 통합 테스트 (MSSQL)")
class BranchRiskMapperTest extends AbstractMssqlTest {

    @Autowired
    private BranchRiskMapper branchRiskMapper;

    @Test
    @DisplayName("폐점 위험 지점 분석 데이터를 정상적으로 조회한다")
    void selectAtRiskBranches_ShouldReturnData() {
        // given

        // when
        List<BranchRiskDTO> result = branchRiskMapper.selectAtRiskBranches();

        // then
        // We just ensure the query syntax is correct for MSSQL.
        assertThat(result).isNotNull();
    }
}
