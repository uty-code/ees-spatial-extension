package com.ees.eval.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import com.ees.eval.service.impl.DifficultyEvaluationServiceImpl;

@ExtendWith(MockitoExtension.class)
class DifficultyEvaluationServiceTest {

    @InjectMocks
    private DifficultyEvaluationServiceImpl difficultyEvaluationService;

    @Test
    @DisplayName("HIGH 밀집도일 경우 1.05 계수가 적용된다")
    void getCoefficientForHigh() {
        BigDecimal coefficient = difficultyEvaluationService.getDifficultyCoefficient("HIGH");
        assertThat(coefficient).isEqualTo(new BigDecimal("1.05"));
    }

    @Test
    @DisplayName("MID 밀집도일 경우 1.03 계수가 적용된다")
    void getCoefficientForMid() {
        BigDecimal coefficient = difficultyEvaluationService.getDifficultyCoefficient("MID");
        assertThat(coefficient).isEqualTo(new BigDecimal("1.03"));
    }

    @Test
    @DisplayName("LOW 밀집도일 경우 1.00 계수가 적용된다")
    void getCoefficientForLow() {
        BigDecimal coefficient = difficultyEvaluationService.getDifficultyCoefficient("LOW");
        assertThat(coefficient).isEqualTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("HIGH 등급 보정 시 Max Bonus +5점 제한이 적용된다")
    void applyScoreWithBonusCapHigh() {
        // HIGH: coefficient = 1.05, cap = +5.0
        // Base = 120, Raw = 126.00, Bonus = 6.0 > 5.0 -> Result = 125.0
        BigDecimal score = difficultyEvaluationService.calculateCappedScore(new BigDecimal("120"), new BigDecimal("1.05"), "HIGH");
        assertThat(score).isEqualTo(new BigDecimal("125.0"));
    }

    @Test
    @DisplayName("MID 등급 보정 시 Max Bonus +3점 제한이 적용된다")
    void applyScoreWithBonusCapMid() {
        // MID: coefficient = 1.03, cap = +3.0
        // Base = 120, Raw = 123.60, Bonus = 3.6 > 3.0 -> Result = 123.0
        BigDecimal score = difficultyEvaluationService.calculateCappedScore(new BigDecimal("120"), new BigDecimal("1.03"), "MID");
        assertThat(score).isEqualTo(new BigDecimal("123.0"));
    }

    @Test
    @DisplayName("보정 상한 미만일 경우 원본 보정치가 적용된다")
    void applyScoreBelowCap() {
        // HIGH: Base = 50, Raw = 52.50, Bonus = 2.5 < 5.0 -> Result = 52.50
        BigDecimal score = difficultyEvaluationService.calculateCappedScore(new BigDecimal("50"), new BigDecimal("1.05"), "HIGH");
        assertThat(score).isEqualTo(new BigDecimal("52.50"));
    }
}
