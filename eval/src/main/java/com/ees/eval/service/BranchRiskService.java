package com.ees.eval.service;

import com.ees.eval.dto.BranchRiskDTO;
import java.util.List;

public interface BranchRiskService {
    List<BranchRiskDTO> getAtRiskBranches();
}
