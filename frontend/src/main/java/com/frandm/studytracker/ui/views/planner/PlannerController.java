package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.controllers.PomodoroController;
import javafx.application.Platform;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlannerController {
    private final DailyTab dailyTab;
    private final WeeklyTab weeklyTab;
    private final PlannerView view;
    private final PomodoroController pomodoroController;
    private LocalDate selectedDate = LocalDate.now();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlannerController(PomodoroController controller) {
        this.pomodoroController = controller;
        this.dailyTab = new DailyTab(controller);
        this.weeklyTab = new WeeklyTab(controller);
        this.dailyTab.setRefreshAction(this::refresh);
        this.weeklyTab.setRefreshAction(this::refresh);
        this.view = new PlannerView(controller, this, dailyTab, weeklyTab);
        refresh();
    }

    public void refresh() {
        LocalDate weekStart = selectedDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        executor.submit(() -> {
            try {
                List<Map<String, Object>> daySessions = loadScheduled(selectedDate, selectedDate);
                List<Map<String, Object>> dayDeadlines = loadDeadlines(selectedDate, selectedDate);
                List<Map<String, Object>> weekSessions = loadScheduled(weekStart, weekEnd);
                List<Map<String, Object>> weekDeadlines = loadDeadlines(weekStart, weekEnd);

                Platform.runLater(() -> {
                    dailyTab.updateHeaderDate(selectedDate);
                    dailyTab.refreshData(daySessions, dayDeadlines);
                    weeklyTab.refreshData(weekStart, weekSessions, weekDeadlines);

                    view.updateTitle();
                    pomodoroController.refreshSideMenu();
                });

            } catch (Exception e) {
                System.err.println("Error refreshing Planner: " + e.getMessage());
            }
        });
    }

    private List<Map<String, Object>> loadScheduled(LocalDate startDate, LocalDate endDate) throws Exception {
        List<Map<String, Object>> sessions = ApiClient.getScheduledSessions(
                format(startDate, LocalTime.MIN),
                format(endDate, LocalTime.MAX)
        );
        process(sessions, "startTime", "endTime");
        return sessions;
    }

    private List<Map<String, Object>> loadDeadlines(LocalDate startDate, LocalDate endDate) throws Exception {
        List<Map<String, Object>> deadlines = ApiClient.getDeadlines(
                format(startDate, LocalTime.MIN),
                format(endDate, LocalTime.MAX)
        );
        process(deadlines, "deadline", null);
        return deadlines;
    }

    private void process(List<Map<String, Object>> items, String startKey, String endKey) {
        if (items == null) return;
        for (Map<String, Object> item : items) {
            LocalDateTime start = resolveStartDate(item, startKey);
            if (start != null) {
                item.put("start_time", start);
                item.put("dueDate", ApiClient.formatApiTimestamp(start));
            }
            item.put("isCompleted", ApiClient.extractCompletedFlag(item));
            if (endKey != null) {
                item.put("end_time", resolveEndDate(item, endKey));
            }

            if (item.containsKey("task") && item.get("task") instanceof Map) {
                Map<?, ?> task = (Map<?, ?>) item.get("task");
                item.put("taskName", task.get("name"));
                item.put("task_name", task.get("name"));
                if (task.containsKey("tag") && task.get("tag") instanceof Map) {
                    Map<?, ?> tag = (Map<?, ?>) task.get("tag");
                    item.put("tagName", tag.get("name"));
                    item.put("tag_name", tag.get("name"));
                    item.put("tagColor", tag.get("color"));
                    item.put("tag_color", tag.get("color"));
                }
            }
        }
    }

    private String format(LocalDate date, LocalTime time) {
        return ApiClient.formatApiTimestamp(date.atTime(time));
    }

    private LocalDateTime resolveStartDate(Map<String, Object> item, String primaryKey) {
        LocalDateTime primary = ApiClient.parseApiTimestamp(item.get(primaryKey));
        if (primary != null) return primary;

        LocalDateTime dueDate = ApiClient.parseApiTimestamp(item.get("dueDate"));
        if (dueDate != null) return dueDate;

        LocalDateTime deadline = ApiClient.parseApiTimestamp(item.get("deadline"));
        if (deadline != null) return deadline;

        return ApiClient.parseApiTimestamp(item.get("startTime"));
    }

    private LocalDateTime resolveEndDate(Map<String, Object> item, String primaryKey) {
        LocalDateTime primary = ApiClient.parseApiTimestamp(item.get(primaryKey));
        return primary != null ? primary : ApiClient.parseApiTimestamp(item.get("endTime"));
    }

    public void nextDay() { move(selectedDate.plusDays(1)); }
    public void prevDay() { move(selectedDate.minusDays(1)); }
    public void nextWeek() { move(selectedDate.plusWeeks(1)); }
    public void prevWeek() { move(selectedDate.minusWeeks(1)); }
    public void today() { move(LocalDate.now()); }

    private void move(LocalDate date) {
        this.selectedDate = date;
        refresh();
    }

    public DailyTab getDailyTab() { return dailyTab; }
    public WeeklyTab getWeeklyTab() { return weeklyTab; }
    public PlannerView getView() { return view; }
}
