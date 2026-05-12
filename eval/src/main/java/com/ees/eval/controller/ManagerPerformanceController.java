package com.ees.eval.controller;

import com.ees.eval.dto.ManagerPerformanceDTO;
import com.ees.eval.service.ManagerPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
public class ManagerPerformanceController {

    private final ManagerPerformanceService managerPerformanceService;

    @GetMapping("/{empId}/performance")
    public ResponseEntity<ManagerPerformanceDTO> getManagerPerformance(@PathVariable Long empId) {
        return ResponseEntity.ok(managerPerformanceService.getPerformanceByManager(empId));
    }
}
