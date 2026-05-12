package com.ees.eval.mapper;

import com.ees.eval.domain.BatchJobHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BatchJobHistoryMapper {
    int insert(BatchJobHistory history);
    int update(BatchJobHistory history);
    BatchJobHistory findById(Long jobId);
    List<BatchJobHistory> findByJobName(@Param("jobName") String jobName);
}
