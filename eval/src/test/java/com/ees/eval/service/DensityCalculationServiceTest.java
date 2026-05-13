package com.ees.eval.service;

import com.ees.eval.domain.RegionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

import com.ees.eval.service.impl.DensityCalculationServiceImpl;

@ExtendWith(MockitoExtension.class)
class DensityCalculationServiceTest {

    @InjectMocks
    private DensityCalculationServiceImpl densityCalculationService;

    @Test
    @DisplayName("URBAN_CORE 밀집도 계산 테스트 (Radius 300m)")
    void calculateDensityForUrbanCore() {
        // given
        RegionType regionType = RegionType.URBAN_CORE;
        
        // when
        String densityLevelLow = densityCalculationService.determineDensityLevel(regionType, 4);
        String densityLevelMid = densityCalculationService.determineDensityLevel(regionType, 5);
        String densityLevelHigh = densityCalculationService.determineDensityLevel(regionType, 9);
        
        // then
        assertThat(densityLevelLow).isEqualTo("LOW");
        assertThat(densityLevelMid).isEqualTo("MID");
        assertThat(densityLevelHigh).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("GENERAL_CITY 밀집도 계산 테스트 (Radius 500m)")
    void calculateDensityForGeneralCity() {
        // given
        RegionType regionType = RegionType.GENERAL_CITY;
        
        // when
        String densityLevelLow = densityCalculationService.determineDensityLevel(regionType, 2);
        String densityLevelMid = densityCalculationService.determineDensityLevel(regionType, 3);
        String densityLevelHigh = densityCalculationService.determineDensityLevel(regionType, 6);
        
        // then
        assertThat(densityLevelLow).isEqualTo("LOW");
        assertThat(densityLevelMid).isEqualTo("MID");
        assertThat(densityLevelHigh).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("SUBURBAN 밀집도 계산 테스트 (Radius 800m)")
    void calculateDensityForSuburban() {
        // given
        RegionType regionType = RegionType.SUBURBAN;
        
        // when
        String densityLevelLow = densityCalculationService.determineDensityLevel(regionType, 1);
        String densityLevelMid = densityCalculationService.determineDensityLevel(regionType, 2);
        String densityLevelHigh = densityCalculationService.determineDensityLevel(regionType, 4);
        
        // then
        assertThat(densityLevelLow).isEqualTo("LOW");
        assertThat(densityLevelMid).isEqualTo("MID");
        assertThat(densityLevelHigh).isEqualTo("HIGH");
    }
}
