package com.ees.eval.controller;

import com.ees.eval.service.EvaluationPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/eval/spatial")
@RequiredArgsConstructor
public class SpatialDashboardController {

    private final EvaluationPeriodService evaluationPeriodService;

    @org.springframework.beans.factory.annotation.Value("${app.kakao.map-key}")
    private String kakaoMapKey;

    @GetMapping
    public String spatialDashboard(Model model) {
        model.addAttribute("activeMenu", "spatial-dashboard");
        
        // Pass Kakao Map Key from application properties
        model.addAttribute("kakaoMapKey", kakaoMapKey);
        
        // Add all periods for the dropdown
        model.addAttribute("periods", evaluationPeriodService.getAllPeriods());
        
        // Get active period if needed
        evaluationPeriodService.getInProgressPeriods().stream().findFirst().ifPresent(p -> {
            model.addAttribute("activePeriodId", p.periodId());
        });
        return "eval/spatial_dashboard";
    }
}
