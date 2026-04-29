package com.ees.eval.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * final_grades_51 테이블에 대응하는 도메인 클래스입니다.
 * 평가 대상자의 기간별 최종 합산 점수 및 등급을 관리합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalGrade {
    private Long gradeId;           // PK
    private Long periodId;          // 평가 기간 ID (FK)
    private Long empId;             // 사원 ID (FK)
    private Integer totalScore;     // 최종 합산 점수 (정수형)
    private String finalGradeCode;  // 최종 등급 코드 (A/B/C 등)
    private String isDeleted;
    private Integer version;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
