package com.ees.eval.mapper;

import com.ees.eval.dto.ManagerPerformanceDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ManagerPerformanceMapper {
    ManagerPerformanceDTO selectPerformanceByManager(Long empId);
    ManagerPerformanceDTO selectPerformanceByManagerAndPeriod(Long empId, Integer year, Integer quarter);
}
