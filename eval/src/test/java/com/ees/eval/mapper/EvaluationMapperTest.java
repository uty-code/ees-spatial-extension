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
    @DisplayName("동적 쿼리를 통한 score1, score2, selfScore 분리 삽입 테스트")
    void testDynamicInsertAndSelect() {
        // Given
        Evaluation evaluation = new Evaluation();
        evaluation.setMappingId(100L);
        evaluation.setElementId(200L);
        // TDD: 아직 Evaluation.java에 없는 필드들을 의도적으로 사용하여 컴파일 에러(실패)를 유도합니다.
        evaluation.setScore1(95);
        evaluation.setReason1("1차 평가 우수");
        evaluation.setConfirmStatusCode("DRAFT");
        evaluation.setIsDeleted("n");
        evaluation.setVersion(0);
        evaluation.setCreatedAt(LocalDateTime.now());

        // When
        evaluationMapper.insert(evaluation);
        Evaluation saved = evaluationMapper.findByMappingIdAndElementId(100L, 200L).orElse(null);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getScore1()).isEqualTo(95);
        assertThat(saved.getScore2()).isNull(); // 2차 평가는 아직 안들어갔으므로 null이어야 함
        assertThat(saved.getReason1()).isEqualTo("1차 평가 우수");
    }
}
