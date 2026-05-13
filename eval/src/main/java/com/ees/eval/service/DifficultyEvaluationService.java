package com.ees.eval.service;

import java.math.BigDecimal;

public interface DifficultyEvaluationService {
    BigDecimal getDifficultyCoefficient(String densityLevel);
    BigDecimal calculateCappedScore(BigDecimal baseScore, BigDecimal coefficient, String densityLevel);
}
