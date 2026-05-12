package com.ees.eval.controller;

import com.ees.eval.dto.BranchRiskDTO;
import com.ees.eval.service.BranchRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/branches/risk")
@RequiredArgsConstructor
public class BranchRiskController {

    private final BranchRiskService branchRiskService;

    @GetMapping
    public ResponseEntity<List<BranchRiskDTO>> getAtRiskBranches() {
        return ResponseEntity.ok(branchRiskService.getAtRiskBranches());
    }
}
