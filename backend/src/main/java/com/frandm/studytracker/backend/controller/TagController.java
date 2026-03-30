package com.frandm.studytracker.backend.controller;

import com.frandm.studytracker.backend.model.Tag;
import com.frandm.studytracker.backend.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<Tag> list() {
        return tagService.getAll();
    }

    @GetMapping("/{id}")
    public Tag get(@PathVariable Long id) {
        return tagService.getById(id);
    }

    @PostMapping
    public Tag create(@RequestBody Map<String, String> body) {
        return tagService.getOrCreate(body.get("name"), body.get("color"));
    }

    @PutMapping("/{id}")
    public Tag update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return tagService.fullUpdate(id, body.get("name"), body.get("color"));
    }

    @PatchMapping("/{id}")
    public Tag patch(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return tagService.partialUpdate(id, body.get("name"), body.get("color"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.ok().build();
    }
}
