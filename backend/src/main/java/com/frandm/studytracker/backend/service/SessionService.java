package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.Session;
import com.frandm.studytracker.backend.model.Task;
import com.frandm.studytracker.backend.repository.SessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final TaskService taskService;

    public SessionService(SessionRepository sessionRepository, TaskService taskService) {
        this.sessionRepository = sessionRepository;
        this.taskService = taskService;
    }

    public Page<Session> getFiltered(String tag, String task, int page, int size) {
        return sessionRepository.findFiltered(
                tag == null || tag.isEmpty() ? null : tag,
                task == null || task.isEmpty() ? null : task,
                PageRequest.of(page, size)
        );
    }

    public List<Session> getAll() {
        return sessionRepository.findAll();
    }

    public List<Session> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findByDateRange(start, end);
    }

    public Session save(String tagName, String tagColor, String taskName,
                        String title, String description,
                        Integer totalMinutes, LocalDateTime startDate,
                        LocalDateTime endDate, Integer rating) {
        Task task = taskService.getOrCreate(tagName, tagColor, taskName);
        Session session = new Session();
        session.setTask(task);
        session.setTitle(title);
        session.setDescription(description);
        session.setTotalMinutes(totalMinutes);
        session.setStartDate(startDate);
        session.setEndDate(endDate);
        session.setRating(rating);
        return sessionRepository.save(session);
    }

    public Session update(Long id, Long taskId, String title,
                          String description, Integer rating) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setTitle(title);
        session.setDescription(description);
        session.setRating(rating);
        return sessionRepository.save(session);
    }

    public void delete(Long id) {
        sessionRepository.deleteById(id);
    }
}