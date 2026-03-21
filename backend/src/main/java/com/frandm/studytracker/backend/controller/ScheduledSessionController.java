package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.ScheduledSession;
import com.frandm.studytracker.backend.service.ScheduledSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduled")
@CrossOrigin
public class ScheduledSessionController {

    private final ScheduledSessionService scheduledSessionService;

    public ScheduledSessionController(ScheduledSessionService scheduledSessionService) {
        this.scheduledSessionService = scheduledSessionService;
    }

    @GetMapping
    public List<ScheduledSession> getByRange(
            @RequestParam String start,
            @RequestParam String end) {
        return scheduledSessionService.getByDateRange(
                LocalDateTime.parse(start),
                LocalDateTime.parse(end)
        );
    }

    @PostMapping
    public ScheduledSession save(@RequestBody Map<String, Object> body) {
        return scheduledSessionService.save(
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                (String) body.get("title"),
                LocalDateTime.parse((String) body.get("startTime")),
                LocalDateTime.parse((String) body.get("endTime"))
        );
    }

    @PutMapping("/{id}")
    public ScheduledSession update(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body) {
        return scheduledSessionService.update(
                id,
                (String) body.get("tagName"),
                (String) body.get("taskName"),
                LocalDateTime.parse((String) body.get("startTime")),
                LocalDateTime.parse((String) body.get("endTime"))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduledSessionService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> markCompleted(@PathVariable Long id) {
        scheduledSessionService.markCompleted(id);
        return ResponseEntity.ok().build();
    }
}