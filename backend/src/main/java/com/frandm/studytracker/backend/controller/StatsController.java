package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.service.StatsService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/heatmap")
    public Map<LocalDate, Integer> getHeatmap() {
        return statsService.getHeatmap();
    }

    @GetMapping("/summary")
    public Map<String, Integer> getSummaryByTag(@RequestParam String tag) {
        return statsService.getSummaryByTag(tag);
    }
}