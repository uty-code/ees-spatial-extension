package com.ees.eval.service.impl;

import com.ees.eval.dto.BranchRiskDTO;
import com.ees.eval.mapper.BranchRiskMapper;
import com.ees.eval.service.BranchRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchRiskServiceImpl implements BranchRiskService {

    private final BranchRiskMapper branchRiskMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BranchRiskDTO> getAtRiskBranches() {
        log.info("Fetching at-risk branches based on revenue and claim criteria");
        return branchRiskMapper.selectAtRiskBranches();
    }
}
