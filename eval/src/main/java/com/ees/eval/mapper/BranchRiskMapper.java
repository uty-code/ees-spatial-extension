package com.ees.eval.mapper;

import com.ees.eval.dto.BranchRiskDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BranchRiskMapper {
    List<BranchRiskDTO> selectAtRiskBranches();
}
