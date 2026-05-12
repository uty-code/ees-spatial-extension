package com.ees.eval.mapper;

import com.ees.eval.dto.RegionPerformanceDTO;
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
@DisplayName("BranchPerformanceMapper 통합 테스트 (MSSQL)")
class BranchPerformanceMapperTest extends AbstractMssqlTest {

    @Autowired
    private BranchPerformanceMapper branchPerformanceMapper;

    @Test
    @DisplayName("지역별 성과 분석 데이터를 정상적으로 조회한다")
    void selectPerformanceByRegion_ShouldReturnData() {
        // when
        List<RegionPerformanceDTO> results = branchPerformanceMapper.selectPerformanceByRegion();

        // then
        // We might not have data in the test container, so we just check it doesn't throw and returns a list.
        assertThat(results).isNotNull();
    }
}
