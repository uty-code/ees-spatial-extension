package com.ees.eval.mapper;

import com.ees.eval.dto.RegionPerformanceDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BranchPerformanceMapper {
    List<RegionPerformanceDTO> selectPerformanceByRegion();
}
