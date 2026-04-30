package com.ees.eval.mapper;

import com.ees.eval.domain.Evaluation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class EvaluationMapperTest {

    @Autowired
    private EvaluationMapper evaluationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("ALTER TABLE evaluations_51 NOCHECK CONSTRAINT ALL");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("ALTER TABLE evaluations_51 CHECK CONSTRAINT ALL");
    }

    @Test
    @DisplayName("통합 score 및 reason 컬럼 단일 삽입 테스트 (TDD Red)")
    void testUnifiedInsertAndSelect() {
        // Given
        Evaluation evaluation = new Evaluation();
        evaluation.setMappingId(100L);
        evaluation.setElementId(200L);
        evaluation.setScore(95);
        evaluation.setReason("단일 컬럼 통합 테스트 우수");
        evaluation.setConfirmStatusCode("DRAFT");
        evaluation.setIsDeleted("n");
        evaluation.setVersion(0);
        evaluation.setCreatedAt(LocalDateTime.now());

        // When
        evaluationMapper.insert(evaluation);
        Evaluation saved = evaluationMapper.findByMappingIdAndElementId(100L, 200L).orElse(null);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getScore()).isEqualTo(95);
        assertThat(saved.getReason()).isEqualTo("단일 컬럼 통합 테스트 우수");
    }
}
