package com.ees.eval.service.impl;

import com.ees.eval.service.DifficultyEvaluationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DifficultyEvaluationServiceImpl implements DifficultyEvaluationService {

    @Override
    public BigDecimal getDifficultyCoefficient(String densityLevel) {
        if ("HIGH".equals(densityLevel)) {
            return new BigDecimal("1.05");
        } else if ("MID".equals(densityLevel)) {
            return new BigDecimal("1.03");
        }
        return new BigDecimal("1.00");
    }

    @Override
    public BigDecimal calculateCappedScore(BigDecimal baseScore, BigDecimal coefficient, String densityLevel) {
        BigDecimal calculatedScore = baseScore.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonus = calculatedScore.subtract(baseScore);
        
        BigDecimal cap = BigDecimal.ZERO;
        if ("HIGH".equals(densityLevel)) {
            cap = new BigDecimal("5.0");
        } else if ("MID".equals(densityLevel)) {
            cap = new BigDecimal("3.0");
        }

        if (bonus.compareTo(cap) > 0) {
            return baseScore.add(cap).setScale(1, RoundingMode.HALF_UP);
        }
        return calculatedScore;
    }
}
