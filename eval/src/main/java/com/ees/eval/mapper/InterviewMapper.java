package com.ees.eval.mapper;

import com.ees.eval.domain.Interview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * interviews_51 테이블에 대한 MyBatis 매퍼 인터페이스입니다.
 */
@Mapper
public interface InterviewMapper {

    /**
     * 매핑 ID로 면담 기록을 조회합니다.
     */
    Optional<Interview> findByMappingId(@Param("mappingId") Long mappingId);

    /**
     * 신규 면담 기록을 삽입합니다.
     */
    void insert(Interview interview);

    /**
     * 기존 면담 기록을 업데이트합니다.
     */
    void update(Interview interview);
}
