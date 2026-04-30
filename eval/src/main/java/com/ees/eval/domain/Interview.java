package com.ees.eval.domain;

import lombok.*;

/**
 * interviews_51 테이블에 대응하는 도메인 클래스입니다.
 * 부서장의 서술형 정성평가(면담 기록)를 관리합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview extends BaseEntity {

    private Long interviewId;      // PK
    private Long mappingId;        // 평가자 매핑 ID (FK)
    private String content1;       // 1. 성과 및 기여도
    private String content2;       // 2. 피드백 주고받기
    private String content3;       // 3. 역량 개발 및 커리어 플랜
    private String content4;       // 4. 다음 목표 설정
    private String statusCode;     // 상태 코드 (DRAFT, COMPLETED)
}
