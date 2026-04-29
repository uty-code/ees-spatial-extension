package com.ees.eval.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * evaluations_51 테이블에 대응하는 도메인 클래스입니다.
 * 피평가자의 각 평가요소에 대한 점수, 코멘트, 확정 상태를 관리합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation extends BaseEntity {

    private Long evalId;          // PK
    private Long mappingId;       // 평가자 매핑 ID (FK)
    private Long elementId;       // 평가 요소 ID (FK)
    private Integer score1;       // 1차 평가 점수 (정수형)
    private Integer score2;       // 2차 평가 점수 (정수형)
    private Integer selfScore;    // 자가 평가 점수 (정수형)
    private String reason1;       // 1차 평가 사유
    private String reason2;       // 2차 평가 사유
    private String confirmStatusCode;     // 확정 상태 코드 (DRAFT/SUBMITTED/CONFIRMED)
}
