package com.ees.eval.mapper;

import com.ees.eval.dto.BranchDensityDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BranchDensityMapper {
    List<BranchDensityDTO.NearbyBranchDTO> selectNearbyBranches(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters
    );
}
