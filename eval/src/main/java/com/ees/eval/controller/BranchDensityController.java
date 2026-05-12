package com.ees.eval.controller;

import com.ees.eval.dto.BranchDensityDTO;
import com.ees.eval.service.BranchDensityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branches/density")
@RequiredArgsConstructor
public class BranchDensityController {

    private final BranchDensityService branchDensityService;

    @GetMapping
    public ResponseEntity<BranchDensityDTO> getBranchDensity(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam double radius) {
        return ResponseEntity.ok(branchDensityService.getBranchDensity(lat, lng, radius));
    }
}
