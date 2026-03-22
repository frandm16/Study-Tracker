package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.controllers.PomodoroController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import org.kordamp.ikonli.javafx.FontIcon;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class WeeklyTab extends VBox {

    private GridPane calendarGrid;
    private GridPane headerGrid;
    private ScrollPane scrollPane;
    private LocalDate currentWeekStart;
    private final PomodoroController controller;
    private Popup activePopup = null;
    private long lastPopupCloseTime = 0;
    private final double ROW_HEIGHT = 60.0;
    private final Pane[] dayColumns = new Pane[7];
    private final VBox[] deadlineContainers = new VBox[7];
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final double MIN_BLOCK_HEIGHT = 30.0;

    public WeeklyTab(PomodoroController controller) {
        this.currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        this.controller = controller;
        this.getStyleClass().add("calendar-root");
        VBox.setVgrow(this, Priority.ALWAYS);
        initializeUI();
    }

    private void initializeUI() {
        headerGrid = new GridPane();
        headerGrid.getStyleClass().add("calendar-header-grid");

        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("calendar-grid");

        scrollPane = new ScrollPane(calendarGrid);
        scrollPane.getStyleClass().add("main-scroll");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(headerGrid, scrollPane);
        refresh();
        Platform.runLater(this::scrollToCurrentTime);
    }

    public void refresh() {
        calendarGrid.getChildren().clear();
        setupGridConstraints();
        renderBaseGrid();
        drawContent();
        drawTimeIndicator();
    }

    private void setupGridConstraints() {
        for (GridPane grid : new GridPane[]{headerGrid, calendarGrid}) {
            grid.getColumnConstraints().clear();
            grid.getColumnConstraints().add(new ColumnConstraints(55));
            for (int i = 0; i < 7; i++) {
                ColumnConstraints dayCol = new ColumnConstraints();
                dayCol.setHgrow(Priority.ALWAYS);
                dayCol.setMinWidth(100);
                grid.getColumnConstraints().add(dayCol);
            }
        }
    }

    private void renderBaseGrid() {
        LocalDate today = LocalDate.now();
        headerGrid.getChildren().clear();

        Region corner = new Region();
        headerGrid.add(corner, 0, 0);

        Pane timeColumn = new Pane();
        timeColumn.setPrefWidth(55);
        calendarGrid.add(timeColumn, 0, 0);

        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            boolean isToday = date.equals(today);
            headerGrid.add(createDayHeader(date, isToday), i + 1, 0);

            Pane columnCanvas = new Pane();
            columnCanvas.getStyleClass().add(isToday ? "calendar-cell-today" : "calendar-column-canvas");
            columnCanvas.setPrefHeight(ROW_HEIGHT * 24);
            columnCanvas.setMinHeight(ROW_HEIGHT * 24);
            dayColumns[i] = columnCanvas;

            VBox deadlineBox = new VBox(2);
            deadlineBox.setPadding(new Insets(5));
            deadlineBox.setPickOnBounds(false);
            deadlineBox.prefWidthProperty().bind(columnCanvas.widthProperty());
            deadlineContainers[i] = deadlineBox;
            columnCanvas.getChildren().add(deadlineBox);

            for (int h = 0; h < 24; h++) {
                if (i == 0) {
                    Label lblHour = new Label(String.format("%02d:00", h));
                    lblHour.getStyleClass().add("calendar-hour-label");
                    lblHour.setLayoutY(h * ROW_HEIGHT - 7);
                    lblHour.setLayoutX(10);
                    timeColumn.getChildren().add(lblHour);
                }

                Region hourCell = new Region();
                hourCell.getStyleClass().add("calendar-cell");
                hourCell.setPrefSize(2000, ROW_HEIGHT);
                hourCell.setLayoutY(h * ROW_HEIGHT);
                hourCell.getStyleClass().add("calendar-hour-cell");

                final int finalH = h;
                hourCell.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1) {
                        showPopup(null, date, e.getScreenX(), e.getScreenY(), finalH, 0, false);
                        e.consume();
                    }
                });
                columnCanvas.getChildren().add(hourCell);
            }
            calendarGrid.add(columnCanvas, i + 1, 0);
        }
    }

    private void drawContent() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startRange = currentWeekStart.atStartOfDay().format(fmt);
        String endRange = currentWeekStart.plusDays(6).atTime(23, 59, 59).format(fmt);

        try {
            List<Map<String, Object>> deadlines = ApiClient.getDeadlines(startRange, endRange);
            for (Map<String, Object> d : deadlines) {
                renderDeadline(d);
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> sessions;
        try {
            sessions = ApiClient.getScheduledSessions(startRange, endRange);
        } catch (Exception e) {
            sessions = new ArrayList<>();
        }

        Map<Integer, List<Map<String, Object>>> dayMap = new HashMap<>();
        for (Map<String, Object> s : sessions) {
            String startStr = getStartTime(s);
            if (startStr == null) continue;
            LocalDateTime start = startStr.contains("T") ? LocalDateTime.parse(startStr) : LocalDateTime.parse(startStr, fmt);
            s.put("start_time", start);

            String endStr = getEndTime(s);
            if (endStr != null) {
                s.put("end_time", endStr.contains("T") ? LocalDateTime.parse(endStr) : LocalDateTime.parse(endStr, fmt));
            }

            s.put("task_name", getTaskName(s));
            s.put("tag_name", getTagName(s));
            s.put("tag_color", getTagColor(s));

            int dayIdx = start.toLocalDate().getDayOfWeek().getValue() - 1;
            dayMap.computeIfAbsent(dayIdx, _ -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : dayMap.entrySet()) {
            int dayIdx = entry.getKey();
            List<Map<String, Object>> daySessions = entry.getValue();
            daySessions.sort(Comparator.comparing(s -> (LocalDateTime) s.get("start_time")));

            List<List<Map<String, Object>>> groups = new ArrayList<>();
            for (Map<String, Object> s : daySessions) {
                boolean placed = false;
                for (List<Map<String, Object>> group : groups) {
                    if (overlapsWithGroup(s, group)) {
                        group.add(s);
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    List<Map<String, Object>> newGroup = new ArrayList<>();
                    newGroup.add(s);
                    groups.add(newGroup);
                }
            }

            for (List<Map<String, Object>> group : groups) {
                int groupSize = group.size();
                for (int i = 0; i < groupSize; i++) {
                    renderSession(group.get(i), dayIdx, i, groupSize);
                }
            }
        }
    }

    private void renderDeadline(Map<String, Object> d) {
        try {
            String taskName = (String) d.get("task_name");
            String deadlineStr = (String) d.get("deadline");
            LocalDateTime date = deadlineStr.contains("T") ? LocalDateTime.parse(deadlineStr) : LocalDateTime.parse(deadlineStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            int dayIdx = date.toLocalDate().getDayOfWeek().getValue() - 1;
            if (dayIdx < 0 || dayIdx > 6) return;

            HBox pill = new HBox(4);
            pill.setAlignment(Pos.CENTER_LEFT);
            pill.getStyleClass().add("calendar-deadline-pill");
            pill.setStyle("-fx-background-color: #ef444420; -fx-border-color: #ef4444; -fx-border-width: 0 0 0 3; -fx-padding: 2 6; -fx-background-radius: 4;");

            FontIcon icon = new FontIcon("mdi2a-alert-circle");
            icon.setStyle("-fx-icon-color: #ef4444; -fx-icon-size: 12;");

            Label lbl = new Label(taskName);
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

            pill.getChildren().addAll(icon, lbl);
            Tooltip.install(pill, new Tooltip("DEADLINE: " + taskName + "\n" + date.format(DateTimeFormatter.ofPattern("HH:mm"))));

            deadlineContainers[dayIdx].getChildren().add(pill);
        } catch (Exception ignored) {}
    }

    private boolean overlapsWithGroup(Map<String, Object> s, List<Map<String, Object>> group) {
        LocalDateTime sStart = (LocalDateTime) s.get("start_time");
        LocalDateTime sEnd = (LocalDateTime) s.get("end_time");
        if (sEnd == null) sEnd = sStart.plusMinutes(25);

        double sTop = (sStart.getHour() * ROW_HEIGHT) + (sStart.getMinute() * (ROW_HEIGHT / 60.0));
        double sBottom = Math.max(sTop + MIN_BLOCK_HEIGHT, (sEnd.getHour() * ROW_HEIGHT) + (sEnd.getMinute() * (ROW_HEIGHT / 60.0)));

        for (Map<String, Object> other : group) {
            LocalDateTime oStart = (LocalDateTime) other.get("start_time");
            LocalDateTime oEnd = (LocalDateTime) other.get("end_time");
            if (oEnd == null) oEnd = oStart.plusMinutes(25);
            double oTop = (oStart.getHour() * ROW_HEIGHT) + (oStart.getMinute() * (ROW_HEIGHT / 60.0));
            double oBottom = Math.max(oTop + MIN_BLOCK_HEIGHT, (oEnd.getHour() * ROW_HEIGHT) + (oEnd.getMinute() * (ROW_HEIGHT / 60.0)));
            if (sTop < oBottom && sBottom > oTop) return true;
        }
        return false;
    }

    private void renderSession(Map<String, Object> s, int dayIdx, int posInGroup, int totalInGroup) {
        String taskName = (String) s.get("task_name");
        String tagName = (String) s.get("tag_name");
        String title = (String) s.get("title");
        LocalDateTime start = (LocalDateTime) s.get("start_time");
        LocalDateTime end = (LocalDateTime) s.get("end_time");
        if (end == null) end = start.plusHours(1);

        String color = s.getOrDefault("tag_color", "#94a3b8").toString();
        double height = Duration.between(start, end).toMinutes() * (ROW_HEIGHT / 60.0);
        VBox block = createSessionBlock(title != null && !title.isEmpty() ? title : taskName, tagName, color, start, end, height);

        block.setOnMouseClicked(e -> {
            showPopup(s, start.toLocalDate(), e.getScreenX(), e.getScreenY(), start.getHour(), start.getMinute(), true);
            e.consume();
        });

        double yStart = (start.getHour() * ROW_HEIGHT) + (start.getMinute() * (ROW_HEIGHT / 60.0));
        block.setLayoutY(yStart);
        block.setPrefHeight(height - 2);
        block.prefWidthProperty().bind(dayColumns[dayIdx].widthProperty().divide(totalInGroup).subtract(2));
        block.layoutXProperty().bind(dayColumns[dayIdx].widthProperty().divide(totalInGroup).multiply(posInGroup).add(1));

        dayColumns[dayIdx].getChildren().add(block);
    }

    private VBox createSessionBlock(String title, String tag, String color, LocalDateTime start, LocalDateTime end, double blockHeight) {
        VBox sessionBlock = new VBox(2);
        sessionBlock.getStyleClass().add("calendar-session-block");
        sessionBlock.setStyle("-fx-border-color: " + color + "; -fx-background-color: " + color + "40; -fx-border-width: 0 0 0 3; -fx-background-radius: 4; -fx-border-radius: 0 4 4 0;");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("calendar-session-task");
        titleLabel.setStyle("-fx-font-weight: bold;");
        Label tagLabel = new Label(tag);
        tagLabel.getStyleClass().add("calendar-session-task");
        tagLabel.setStyle("-fx-opacity: 0.9;");

        if (blockHeight < 40) {
            tagLabel.setVisible(false);
            tagLabel.setManaged(false);
        }

        sessionBlock.getChildren().addAll(titleLabel, tagLabel);
        Tooltip.install(sessionBlock, new Tooltip(title + "\n" + start.format(timeFormatter) + " - " + end.format(timeFormatter)));
        return sessionBlock;
    }

    private VBox createDayHeader(LocalDate date, boolean isToday) {
        Label lblName = new Label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase());
        lblName.getStyleClass().add(isToday ? "calendar-day-name-today" : "calendar-day-name");
        Label lblNum = new Label(String.valueOf(date.getDayOfMonth()));
        lblNum.getStyleClass().add(isToday ? "calendar-day-num-today" : "calendar-day-num");
        StackPane numStack = new StackPane();
        if (isToday) {
            Circle c = new Circle(14);
            c.getStyleClass().add("calendar-today-circle");
            numStack.getChildren().add(c);
        }
        numStack.getChildren().add(lblNum);
        VBox v = new VBox(2, lblName, numStack);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(10, 0, 10, 0));
        return v;
    }

    private void drawTimeIndicator() {
        LocalDate today = LocalDate.now();
        int dayIdx = today.getDayOfWeek().getValue() - 1;
        if (currentWeekStart.equals(today.with(DayOfWeek.MONDAY)) && dayIdx >= 0 && dayIdx < 7) {
            double yPos = (LocalTime.now().getHour() * ROW_HEIGHT) + (LocalTime.now().getMinute() * (ROW_HEIGHT / 60.0));
            HBox timeIndicator = new HBox();
            timeIndicator.setAlignment(Pos.CENTER_LEFT);
            timeIndicator.setMouseTransparent(true);
            timeIndicator.setLayoutY(yPos - 4);

            Circle dot = new Circle(4);
            dot.setStyle("-fx-fill: #e74c3c;");
            Region line = new Region();
            line.setStyle("-fx-background-color: #e74c3c; -fx-min-height: 2px; -fx-max-height: 2px;");
            HBox.setHgrow(line, Priority.ALWAYS);

            timeIndicator.getChildren().addAll(dot, line);
            timeIndicator.prefWidthProperty().bind(dayColumns[dayIdx].widthProperty());
            dayColumns[dayIdx].getChildren().add(timeIndicator);
        }
    }

    public void scrollToCurrentTime() {
        double currentHour = LocalTime.now().getHour();
        scrollPane.setVvalue((currentHour > 3) ? (currentHour - 3) / 24.0 : 0);
    }

    public String getHeaderTitle() {
        String m = currentWeekStart.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        return m.substring(0, 1).toUpperCase() + m.substring(1) + " " + currentWeekStart.getYear();
    }

    public void setCurrentWeekStart(LocalDate date) {
        this.currentWeekStart = date.with(DayOfWeek.MONDAY);
        refresh();
    }

    private void showPopup(Map<String, Object> existingSession, LocalDate date, double screenX, double screenY, int hour, int minute, boolean isSessionClick) {
        long currentTime = System.currentTimeMillis();
        if (!isSessionClick && (currentTime - lastPopupCloseTime < 150)) {
            return;
        }

        if (activePopup != null && activePopup.isShowing()) {
            activePopup.hide();
            activePopup = null;
            lastPopupCloseTime = currentTime;

            if (!isSessionClick) {
                return;
            }
        }

        Popup popup = new Popup();
        activePopup = popup;
        popup.setAutoHide(true);

        VBox root = new VBox(12);
        String currentTheme = controller.getCurrentTheme();
        root.getStyleClass().addAll("calendar-popup", currentTheme);

        root.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/frandm/studytracker/css/styles.css")).toExternalForm());

        root.setPadding(new Insets(20));
        root.setPrefWidth(350);
        root.setStyle("-fx-background-color: -color-bg-overlay; -fx-border-color: -color-border-default; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label titleLabel = new Label(existingSession == null ? "Schedule Session" : "Edit Scheduled Session");
        titleLabel.getStyleClass().add("title-schedule-session");

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Title here...");
        if (existingSession != null) {
            txtTitle.setText((String) existingSession.getOrDefault("title", ""));
        }
        txtTitle.getStyleClass().add("input-calendar");

        String initialTask = existingSession != null ? (String)existingSession.get("task_name") : null;
        LocalDateTime startDt = existingSession != null ? (LocalDateTime)existingSession.get("start_time") : date.atTime(hour, minute);
        LocalDateTime endDt = existingSession != null ? (LocalDateTime)existingSession.get("end_time") : startDt.plusHours(1);

        TextField hStart = createTimeField(String.format("%02d", startDt.getHour()), 23);
        TextField mStart = createTimeField(String.format("%02d", startDt.getMinute()), 59);
        TextField hEnd = createTimeField(String.format("%02d", endDt.getHour()), 23);
        TextField mEnd = createTimeField(String.format("%02d", endDt.getMinute()), 59);

        HBox timeRow = new HBox(10);

        timeRow.setAlignment(Pos.CENTER);
        HBox inicio = new HBox(3, hStart, new Label(":"), mStart);
        inicio.setAlignment(Pos.CENTER);
        HBox fin = new HBox(3, hEnd, new Label(":"), mEnd);
        fin.setAlignment(Pos.CENTER);
        timeRow.getChildren().addAll(inicio, new Label("to"), fin);


        ComboBox<String> comboTags = new ComboBox<>();
        ComboBox<String> comboTasks = new ComboBox<>();
        comboTags.setMaxWidth(Double.MAX_VALUE);
        comboTasks.setMaxWidth(Double.MAX_VALUE);

        final Map<String, List<String>> tagTasksMap = new LinkedHashMap<>();
        try {
            ApiClient.getTags().forEach(t -> {
                String tagName = (String) t.get("name");
                try {
                    List<String> tasks = ApiClient.getTasksByTag(tagName).stream()
                            .map(task -> (String) task.get("name"))
                            .collect(Collectors.toList());
                    tagTasksMap.put(tagName, tasks);
                } catch (Exception e) {
                    tagTasksMap.put(tagName, new ArrayList<>());
                }
            });
        } catch (Exception e) {
            System.err.println("Error loading tags: " + e.getMessage());
        }
        comboTags.getItems().addAll(tagTasksMap.keySet());

        comboTags.setOnAction(_ -> {
            comboTasks.getItems().clear();
            List<String> tasks = tagTasksMap.get(comboTags.getValue());
            if (tasks != null) comboTasks.getItems().addAll(tasks);
        });

        if (existingSession != null) {
            String tagName = tagTasksMap.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(initialTask))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (tagName != null) {
                comboTags.getSelectionModel().select(tagName);
                comboTasks.getItems().addAll(tagTasksMap.get(tagName));
                comboTasks.getSelectionModel().select(initialTask);
            }
        }

        DatePicker datePicker = new DatePicker(startDt.toLocalDate());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.getStyleClass().add("input-calendar");

        Button btnSave = new Button(existingSession == null ? "Save" : "Update");
        btnSave.getStyleClass().add("button-primary");
        btnSave.setMaxWidth(Double.MAX_VALUE);

        btnSave.setOnAction(_ -> {
            if (comboTasks.getValue() == null || comboTags.getValue() == null) return;
            int hs = hStart.getText().isEmpty() ? 0 : Integer.parseInt(hStart.getText());
            int ms = mStart.getText().isEmpty() ? 0 : Integer.parseInt(mStart.getText());
            int he = hEnd.getText().isEmpty() ? 0 : Integer.parseInt(hEnd.getText());
            int me = mEnd.getText().isEmpty() ? 0 : Integer.parseInt(mEnd.getText());

            LocalDateTime finalStart = datePicker.getValue().atTime(hs, ms);
            LocalDateTime finalEnd = datePicker.getValue().atTime(he, me);

            if (finalEnd.isBefore(finalStart)) {
                LocalDateTime temp = finalStart;
                finalStart = finalEnd;
                finalEnd = temp;
            }

            if (finalStart.equals(finalEnd)) {
                finalEnd = finalEnd.plusMinutes(1);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String sessionTitle = txtTitle.getText().trim();
            if (sessionTitle.isEmpty()) sessionTitle = "";

            if (existingSession == null) {
                try {
                    ApiClient.saveScheduledSession(
                            comboTags.getValue(),
                            comboTasks.getValue(),
                            sessionTitle,
                            finalStart.format(fmt),
                            finalEnd.format(fmt)
                    );
                } catch (Exception e) {
                    System.err.println("Error saving scheduled session: " + e.getMessage());
                }
            } else {
                try {
                    ApiClient.updateScheduledSession(
                            (int) existingSession.get("id"),
                            comboTags.getValue(),
                            comboTasks.getValue(),
                            finalStart.format(fmt),
                            finalEnd.format(fmt)
                    );
                } catch (Exception e) {
                    System.err.println("Error updating scheduled session: " + e.getMessage());
                }
            }
            popup.hide();
            refresh();
        });

        root.getChildren().addAll(
                titleLabel,
                new Label("Title"), txtTitle,
                new Label("Tag"), comboTags,
                new Label("Task"), comboTasks,
                new Label("Date"), datePicker,
                timeRow,
                btnSave
        );

        if (existingSession != null) {
            Button btnDelete = new Button("Delete");
            btnDelete.getStyleClass().add("button-danger");
            btnDelete.setMaxWidth(Double.MAX_VALUE);
            btnDelete.setOnAction(_ -> {
                try {
                    ApiClient.deleteScheduledSession((int) existingSession.get("id"));
                } catch (Exception e) {
                    System.err.println("Error deleting scheduled session: " + e.getMessage());
                }
                popup.hide();
                refresh();
            });
            root.getChildren().add(btnDelete);
        }

        popup.setOnHidden(_ -> {
            lastPopupCloseTime = System.currentTimeMillis();
            if (activePopup == popup) {
                activePopup = null;
            }
        });

        popup.getContent().add(root);
        popup.show(this.getScene().getWindow(), screenX, screenY);
    }

    private TextField createTimeField(String initial, int max) {
        TextField tf = new TextField(initial);
        tf.setPrefWidth(35);
        tf.setAlignment(Pos.CENTER);
        tf.textProperty().addListener((_, _, newV) -> {
            if (!newV.matches("\\d*")) tf.setText(newV.replaceAll("\\D", ""));
            if (tf.getText().length() > 2) tf.setText(tf.getText().substring(0, 2));
            if (!tf.getText().isEmpty() && Integer.parseInt(tf.getText()) > max) tf.setText(String.valueOf(max));
        });
        return tf;
    }

    private String getStartTime(Map<String, Object> s) { return s.get("startTime") != null ? s.get("startTime").toString() : null; }
    private String getEndTime(Map<String, Object> s) { return s.get("endTime") != null ? s.get("endTime").toString() : null; }
    private String getTaskName(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); return t != null ? (String) t.get("name") : ""; }
    private String getTagName(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); if (t == null) return ""; Map<?, ?> tg = (Map<?, ?>) t.get("tag"); return tg != null ? (String) tg.get("name") : ""; }
    private String getTagColor(Map<String, Object> s) { Map<?, ?> t = (Map<?, ?>) s.get("task"); if (t == null) return "#94a3b8"; Map<?, ?> tg = (Map<?, ?>) t.get("tag"); return tg != null ? (String) tg.get("color") : "#94a3b8"; }
}