package com.ees.eval.mapper;

import com.ees.eval.dto.BranchDensityDTO;
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
@DisplayName("BranchDensityMapper 통합 테스트 (MSSQL)")
class BranchDensityMapperTest extends AbstractMssqlTest {

    @Autowired
    private BranchDensityMapper branchDensityMapper;

    @Test
    @DisplayName("STDistance를 사용하여 반경 내 지점을 정상적으로 조회한다")
    void selectNearbyBranches_ShouldReturnData() {
        // given
        double lat = 37.4979;
        double lng = 127.0276;
        double radiusMeters = 5000.0;

        // when
        List<BranchDensityDTO.NearbyBranchDTO> result = branchDensityMapper.selectNearbyBranches(lat, lng, radiusMeters);

        // then
        // We just ensure the spatial query syntax is correct for MSSQL and executes without errors.
        assertThat(result).isNotNull();
    }
}
