package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.Task;
import com.frandm.studytracker.backend.service.TaskService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getByTag(@RequestParam String tag) {
        return taskService.getByTag(tag);
    }

    @PostMapping
    public Task getOrCreate(@RequestBody Map<String, String> body) {
        return taskService.getOrCreate(
                body.get("tagName"),
                body.get("tagColor"),
                body.get("taskName")
        );
    }
}