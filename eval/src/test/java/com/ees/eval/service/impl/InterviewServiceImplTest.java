package com.ees.eval.service.impl;

import com.ees.eval.domain.Interview;
import com.ees.eval.dto.InterviewDTO;
import com.ees.eval.mapper.InterviewMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    private InterviewMapper interviewMapper;

    @InjectMocks
    private InterviewServiceImpl interviewService;

    @Test
    @DisplayName("should_partition_ids_when_mappingIds_exceed_chunk_size")
    void should_partition_ids_when_mappingIds_exceed_chunk_size() {
        // given: 2500개의 ID 생성 (chunkSize 1000 가정 시 3번 호출되어야 함)
        List<Long> mappingIds = new ArrayList<>();
        for (long i = 1; i <= 2500; i++) {
            mappingIds.add(i);
        }

        // 각 호출마다 일부 데이터 반환하도록 설정
        when(interviewMapper.findByMappingIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            List<Interview> results = new ArrayList<>();
            for (Long id : ids) {
                results.add(Interview.builder().mappingId(id).statusCode("COMPLETED").build());
            }
            return results;
        });

        // when
        Map<Long, InterviewDTO> results = interviewService.getInterviewsByMappingIds(mappingIds);

        // then
        assertThat(results).hasSize(2500);
        // findByMappingIds가 3번 호출되었는지 검증 (1000 + 1000 + 500)
        verify(interviewMapper, times(3)).findByMappingIds(anyList());
    }

    @Test
    @DisplayName("should_return_empty_map_when_mappingIds_is_empty")
    void should_return_empty_map_when_mappingIds_is_empty() {
        // when
        Map<Long, InterviewDTO> results = interviewService.getInterviewsByMappingIds(Collections.emptyList());

        // then
        assertThat(results).isEmpty();
        verify(interviewMapper, never()).findByMappingIds(anyList());
    }
}
