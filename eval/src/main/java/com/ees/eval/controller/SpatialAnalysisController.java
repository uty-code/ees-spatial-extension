package com.ees.eval.controller;

import com.ees.eval.dto.HeatMapDTO;
import com.ees.eval.service.SpatialAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spatial")
@RequiredArgsConstructor
public class SpatialAnalysisController {

    private final SpatialAnalysisService spatialAnalysisService;

    @GetMapping("/heatmap")
    public List<HeatMapDTO> getHeatMap(@RequestParam Long periodId) {
        return spatialAnalysisService.getHeatMapData(periodId);
    }
}
