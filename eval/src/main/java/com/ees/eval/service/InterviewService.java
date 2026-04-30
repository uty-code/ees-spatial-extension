package com.ees.eval.service;

import com.ees.eval.domain.Interview;
import com.ees.eval.dto.InterviewDTO;
import java.util.Optional;

/**
 * 면담 기록(정성평가) 관리를 위한 비즈니스 로직 인터페이스입니다.
 */
public interface InterviewService {

    /**
     * 매핑 ID로 면담 기록을 조회하여 DTO로 반환합니다.
     */
    Optional<InterviewDTO> getInterviewByMappingId(Long mappingId);

    void saveInterview(Long mappingId, String content1, String content2, String content3, String content4, String statusCode, Long empId);
}
