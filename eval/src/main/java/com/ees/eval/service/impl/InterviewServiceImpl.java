package com.ees.eval.service.impl;

import com.ees.eval.domain.Interview;
import com.ees.eval.dto.InterviewDTO;
import com.ees.eval.mapper.InterviewMapper;
import com.ees.eval.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewServiceImpl implements InterviewService {

    private final InterviewMapper interviewMapper;

    @Override
    public Optional<InterviewDTO> getInterviewByMappingId(Long mappingId) {
        return interviewMapper.findByMappingId(mappingId)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional
    public void saveInterview(Long mappingId, String content1, String content2, String content3, String content4, String statusCode, Long empId) {
        interviewMapper.findByMappingId(mappingId)
                .ifPresentOrElse(
                    existing -> {
                        existing.setContent1(content1);
                        existing.setContent2(content2);
                        existing.setContent3(content3);
                        existing.setContent4(content4);
                        existing.setStatusCode(statusCode);
                        existing.preUpdate();
                        existing.setUpdatedBy(empId);
                        interviewMapper.update(existing);
                    },
                    () -> {
                        Interview interview = Interview.builder()
                                .mappingId(mappingId)
                                .content1(content1)
                                .content2(content2)
                                .content3(content3)
                                .content4(content4)
                                .statusCode(statusCode)
                                .build();
                        interview.prePersist();
                        interview.setCreatedBy(empId);
                        interview.setUpdatedBy(empId);
                        interviewMapper.insert(interview);
                    }
                );
    }

    private InterviewDTO convertToDTO(Interview interview) {
        return InterviewDTO.builder()
                .interviewId(interview.getInterviewId())
                .mappingId(interview.getMappingId())
                .content1(interview.getContent1())
                .content2(interview.getContent2())
                .content3(interview.getContent3())
                .content4(interview.getContent4())
                .statusCode(interview.getStatusCode())
                .version(interview.getVersion())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }
}
