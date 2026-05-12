package com.ees.eval.controller;

import com.ees.eval.dto.RegionPerformanceDTO;
import com.ees.eval.service.BranchPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/branches/performance")
@RequiredArgsConstructor
public class BranchPerformanceController {

    private final BranchPerformanceService branchPerformanceService;

    @GetMapping("/by-region")
    public ResponseEntity<List<RegionPerformanceDTO>> getPerformanceByRegion() {
        return ResponseEntity.ok(branchPerformanceService.getPerformanceByRegion());
    }
}
