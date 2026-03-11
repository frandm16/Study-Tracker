package com.frandm.pomodoro;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HistoryView extends StackPane {

    private final PomodoroController controller;
    private final VBox globalHistoryRoot;
    private final VBox focusAreasRoot;
    private final VBox detailRoot;
    private final Button btnGlobalHistory;
    private final Button btnFocusAreas;
    private final VBox sessionsContainer;
    private final VBox tasksSummaryContainer;
    private final Label detailTitleLabel;
    private final Button loadMoreBtn;
    private ComboBox<String> tagFilterCombo;
    private ComboBox<String> taskFilterCombo;

    private String currentTag = null;
    private String currentTask = null;
    private int currentOffset = 0;
    private final int PAGE_SIZE = 50;
    private LocalDate lastDate = null;
    private VBox lastSessionsContainer = null;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HistoryView(Map<String, String> tagColors, PomodoroController controller) {
        this.controller = controller;

        HBox navigationBar = new HBox();
        navigationBar.getStyleClass().add("history-nav-bar");

        btnGlobalHistory = new Button("Global History");
        btnFocusAreas = new Button("Focus Areas");
        btnGlobalHistory.getStyleClass().addAll("title-button", "active");
        btnFocusAreas.getStyleClass().add("title-button");
        navigationBar.getChildren().addAll(btnGlobalHistory, btnFocusAreas);

        globalHistoryRoot = new VBox();
        globalHistoryRoot.getStyleClass().add("history-content-root");

        HBox filterBar = new HBox();
        filterBar.getStyleClass().add("history-filter-bar");

        tagFilterCombo = new ComboBox<>();
        tagFilterCombo.setPromptText("All Tags");

        taskFilterCombo = new ComboBox<>();
        taskFilterCombo.setPromptText("All Tasks");
        taskFilterCombo.setDisable(true);

        Label filterIcon = new Label();
        filterIcon.setGraphic(new FontIcon("mdi2f-filter-variant"));
        filterIcon.getStyleClass().add("filter-label-icon");

        filterBar.getChildren().addAll(filterIcon, tagFilterCombo, taskFilterCombo);

        sessionsContainer = new VBox();
        sessionsContainer.getStyleClass().add("sessions-main-container");

        loadMoreBtn = new Button("Load more");
        loadMoreBtn.getStyleClass().add("button-secondary");
        loadMoreBtn.setOnAction(e -> loadMore());

        VBox scrollContent = new VBox( sessionsContainer, loadMoreBtn);
        scrollContent.getStyleClass().add("history-scroll-content");

        ScrollPane historyScroll = new ScrollPane(scrollContent);
        historyScroll.setFitToWidth(true);
        historyScroll.getStyleClass().add("setup-scroll");
        globalHistoryRoot.getChildren().addAll(filterBar, historyScroll);

        focusAreasRoot = new VBox();
        focusAreasRoot.getStyleClass().add("focus-areas-root");
        focusAreasRoot.setVisible(false);
        focusAreasRoot.setManaged(false);

        detailRoot = new VBox();
        detailRoot.getStyleClass().add("history-detail-root");
        detailRoot.setVisible(false);
        detailRoot.setManaged(false);

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setGraphic(new FontIcon("mdi2a-arrow-left"));
        backBtn.setOnAction(e -> switchTab(false));

        detailTitleLabel = new Label();
        detailTitleLabel.getStyleClass().add("detail-title-label");

        HBox details = new HBox(backBtn, detailTitleLabel);
        details.getStyleClass().add("details-focus-area");

        tasksSummaryContainer = new VBox();
        tasksSummaryContainer.getStyleClass().add("tasks-summary-container");

        ScrollPane detailScroll = new ScrollPane(tasksSummaryContainer);
        detailScroll.setFitToWidth(true);
        detailScroll.getStyleClass().add("setup-scroll");
        detailRoot.getChildren().addAll(details, detailScroll);

        btnGlobalHistory.setOnAction(e -> switchTab(true));
        btnFocusAreas.setOnAction(e -> switchTab(false));

        setupFilterListeners();
        VBox layout = new VBox(navigationBar, globalHistoryRoot, focusAreasRoot, detailRoot);
        layout.getStyleClass().add("history-view-layout");
        this.getChildren().add(layout);

        initData();
    }

    private void initData() {
        refreshFilters();
        resetAndReload();
    }

    private void switchTab(boolean showGlobal) {
        btnGlobalHistory.getStyleClass().remove("active");
        btnFocusAreas.getStyleClass().remove("active");
        globalHistoryRoot.setVisible(showGlobal);
        globalHistoryRoot.setManaged(showGlobal);
        focusAreasRoot.setVisible(!showGlobal);
        focusAreasRoot.setManaged(!showGlobal);
        detailRoot.setVisible(false);
        detailRoot.setManaged(false);

        if (showGlobal) {
            btnGlobalHistory.getStyleClass().add("active");
            resetAndReload();
        } else {
            btnFocusAreas.getStyleClass().add("active");
            refreshFocusAreasGrid();
        }
    }

    private void showTagDetail(String tagName) {
        focusAreasRoot.setVisible(false);
        focusAreasRoot.setManaged(false);
        detailRoot.setVisible(true);
        detailRoot.setManaged(true);
        detailTitleLabel.setText(tagName);
        String color = DatabaseHandler.getTagColors().getOrDefault(tagName, "#ffffff");
        detailTitleLabel.setStyle("-fx-text-fill: " + color + ";");
        loadTagSummary(tagName);
    }

    private void loadTagSummary(String tagName) {
        tasksSummaryContainer.getChildren().clear();
        Map<String, Integer> summary = DatabaseHandler.getTaskSummaryByTag(tagName);
        summary.forEach((task, minutes) -> {
            HBox row = new HBox();
            row.getStyleClass().add("summary-row-card");
            Label name = new Label(task);
            name.getStyleClass().add("summary-task-name");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label time = new Label(minutes + " min");
            time.getStyleClass().add("summary-task-time");
            Button btnPlayTask = new Button();
            FontIcon playIcon = new FontIcon("fas-play");
            btnPlayTask.setGraphic(playIcon);
            btnPlayTask.getStyleClass().add("play-schedule-session");
            btnPlayTask.setOnAction(e -> {
                e.consume();
                controller.playScheduleSession(tagName, task);
                controller.switchToTimer();
            });
            Tooltip ttPlay = new Tooltip("Start task");
            ttPlay.setShowDelay(Duration.millis(75));
            ttPlay.getStyleClass().add("heatmap-tooltip");
            btnPlayTask.setTooltip(ttPlay);

            row.getChildren().addAll(name, spacer, time, btnPlayTask);
            tasksSummaryContainer.getChildren().add(row);
        });
    }

    private void setupFilterListeners() {
        tagFilterCombo.setOnAction(e -> {
            String selected = tagFilterCombo.getValue();
            if (selected == null || selected.equals("All Tags")) {
                currentTag = null;
                currentTask = null;
                taskFilterCombo.setValue("All Tasks");
                taskFilterCombo.setDisable(true);
            } else {
                currentTag = selected;
                currentTask = null;
                updateTaskFilterCombo(currentTag);
                taskFilterCombo.setValue("All Tasks");
                taskFilterCombo.setDisable(false);
            }
            resetAndReload();
        });
        taskFilterCombo.setOnAction(e -> {
            String selected = taskFilterCombo.getValue();
            currentTask = (selected == null || selected.equals("All Tasks")) ? null : selected;
            resetAndReload();
        });
    }

    private void updateTaskFilterCombo(String tagName) {
        taskFilterCombo.getItems().clear();
        taskFilterCombo.getItems().add("All Tasks");
        taskFilterCombo.getItems().addAll(DatabaseHandler.getTasksByTag(tagName));
    }

    private void refreshFilters() {
        tagFilterCombo.getItems().clear();
        tagFilterCombo.getItems().add("All Tags");
        tagFilterCombo.getItems().addAll(DatabaseHandler.getTagColors().keySet());
    }

    public void resetAndReload() {
        currentOffset = 0;
        sessionsContainer.getChildren().clear();

        LocalDate today = LocalDate.now();
        List<Session> allLoaded = DatabaseHandler.getFilteredSessions(currentTag, currentTask, 500, 0);

        long todayTotal = allLoaded.stream()
                .filter(s -> LocalDateTime.parse(s.getStartDate(), DATE_FORMATTER).toLocalDate().equals(today))
                .mapToLong(Session::getTotalMinutes)
                .sum();

        List<Session> todaySessions = allLoaded.stream()
                .filter(s -> LocalDateTime.parse(s.getStartDate(), DATE_FORMATTER).toLocalDate().equals(today))
                .toList();

        String statusMessage = todaySessions.isEmpty() ? "Not registered sessions" : null;

        createNewDayBlock(today, todayTotal, statusMessage);
        lastDate = today;

        if (!todaySessions.isEmpty()) {
            todaySessions.forEach(s -> lastSessionsContainer.getChildren().add(createTimelineCard(s)));
        }

        loadMore();
    }

    private void loadMore() {
        List<Session> sessions = DatabaseHandler.getFilteredSessions(currentTag, currentTask, PAGE_SIZE, currentOffset);

        if (sessions.isEmpty() && currentOffset == 0) {
            Label noSessions = new Label("No sessions found");
            noSessions.getStyleClass().add("no-sessions-label");
            sessionsContainer.getChildren().add(noSessions);
            loadMoreBtn.setVisible(false);
            return;
        }

        for (Session s : sessions) {
            LocalDate sessionDate = LocalDateTime.parse(s.getStartDate(), DATE_FORMATTER).toLocalDate();

            if (!sessionDate.equals(lastDate)) {
                long totalMinutes = sessions.stream()
                        .filter(se -> LocalDateTime.parse(se.getStartDate(), DATE_FORMATTER).toLocalDate().equals(sessionDate))
                        .mapToLong(Session::getTotalMinutes)
                        .sum();

                createNewDayBlock(sessionDate, totalMinutes, null);
                lastDate = sessionDate;
            }
            lastSessionsContainer.getChildren().add(createTimelineCard(s));
        }

        currentOffset += PAGE_SIZE;
        loadMoreBtn.setVisible(sessions.size() == PAGE_SIZE);
    }

    private void createNewDayBlock(LocalDate date, long totalMinutes, String statusMessage) {
        HBox dayHeader = new HBox(15);
        dayHeader.getStyleClass().add("history-day-header");
        dayHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane circle = new StackPane();
        circle.getStyleClass().add("timeline-date-circle");

        VBox dateTextCont = new VBox(-2);
        dateTextCont.setAlignment(Pos.CENTER);
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.getStyleClass().add("timeline-day-num");
        Label dayLabel = new Label(date.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase());
        dayLabel.getStyleClass().add("timeline-day-month");
        dateTextCont.getChildren().addAll(dayNum, dayLabel);
        circle.getChildren().add(dateTextCont);

        VBox dayInfo = new VBox(2);
        dayInfo.setAlignment(Pos.CENTER_LEFT);
        Label dateFull = new Label(date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM")));
        dateFull.getStyleClass().add("day-full-label");

        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        Label totalLabel = new Label(String.format("Total: %dh %02dm", h, m));
        totalLabel.getStyleClass().add("day-total-label");

        dayInfo.getChildren().addAll(dateFull, totalLabel);
        dayHeader.getChildren().addAll(circle, dayInfo);

        if (statusMessage != null) {
            Label statusLabel = new Label(statusMessage);
            statusLabel.getStyleClass().add("today-status-inline");
            dayHeader.getChildren().add(statusLabel);
        }

        lastSessionsContainer = new VBox(15);
        lastSessionsContainer.getStyleClass().add("day-sessions-container-clean");

        sessionsContainer.getChildren().addAll(dayHeader, lastSessionsContainer);
    }

    public void refreshFocusAreasGrid() {
        focusAreasRoot.getChildren().clear();
        GridPane grid = new GridPane();
        grid.getStyleClass().add("focus-areas-grid");

        for (int i = 0; i < 4; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(25);
            grid.getColumnConstraints().add(colConst);
        }

        Map<String, String> updatedTags = DatabaseHandler.getTagColors();
        int col = 0, row = 0;
        for (Map.Entry<String, String> entry : updatedTags.entrySet()) {
            grid.add(createTagCard(entry.getKey(), entry.getValue()), col++, row);
            if (col == 4) { col = 0; row++; }
        }
        focusAreasRoot.getChildren().add(grid);
    }

    private VBox createTagCard(String name, String color) {
        VBox card = new VBox();
        card.getStyleClass().add("tag-explorer-card");
        HBox topRow = new HBox();
        topRow.getStyleClass().add("tag-card-header");
        Region dot = new Region();
        dot.getStyleClass().add("tag-card-dot");
        dot.setStyle("-fx-background-color: " + color + ";");
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("tag-card-name");
        topRow.getChildren().addAll(dot, nameLabel);
        card.getChildren().add(topRow);
        card.setOnMouseClicked(e -> showTagDetail(name));
        return card;
    }

    private VBox createTimelineCard(Session s) {
        VBox card = new VBox();
        card.getStyleClass().add("timeline-card");

        HBox header = new HBox();
        header.getStyleClass().add("timeline-card-header");
        Label sessionTitle = new Label(s.getTitle());
        sessionTitle.getStyleClass().add("timeline-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String start = s.getStartDate().substring(11, 16);
        String end = s.getEndDate().substring(11, 16);
        Label timeRange = new Label(start + " — " + end + " (" + s.getTotalMinutes() + " m)");
        timeRange.getStyleClass().add("timeline-card-time");

        Button optionsBtn = new Button();
        optionsBtn.getStyleClass().add("card-options-button");
        FontIcon optionsIcon = new FontIcon("mdi2d-dots-horizontal");
        optionsIcon.getStyleClass().add("options-icon");
        optionsBtn.setGraphic(optionsIcon);

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("session-context-menu");

        MenuItem editItem = new MenuItem("Edit");
        editItem.setGraphic(new FontIcon("mdi2p-pencil"));
        editItem.setOnAction(e -> {
            controller.openEditSession(s);
        });


        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
        deleteItem.getStyleClass().add("menu-item-delete");
        deleteItem.setOnAction(e -> {
            controller.showDeleteConfirmation(s, this);
        });

        contextMenu.getItems().addAll(editItem, deleteItem);

        optionsBtn.setOnAction(e -> {
            contextMenu.show(optionsBtn, Side.BOTTOM, 0, 0);
        });

        header.getChildren().addAll(sessionTitle, timeRange, spacer, optionsBtn);

        HBox badges = new HBox();
        badges.getStyleClass().add("timeline-card-badges");
        Label tagBadge = new Label(s.getTag());
        tagBadge.getStyleClass().add("task-badge");
        tagBadge.setStyle("-fx-border-color: " + s.getTagColor() + "; -fx-text-fill: " + s.getTagColor() + ";");
        Label taskBadge = new Label(s.getTask());
        taskBadge.getStyleClass().add("task-badge");
        badges.getChildren().addAll(tagBadge, taskBadge);

        VBox details = new VBox(12);
        details.setManaged(false);
        details.setVisible(false);
        details.setPadding(new Insets(10, 0, 0, 0));

        HBox stars = new HBox();
        stars.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 5; i++) {

            FontIcon star = new FontIcon("fas-star");
            star.setIconSize(10);
            star.setCursor(javafx.scene.Cursor.HAND);

            if (i <= s.getRating()) {
                star.getStyleClass().add("selectedStarHistory");
            } else {
                star.getStyleClass().add("unselectedStarHistory");
            }
            stars.getChildren().add(star);
        }
        Label desc = new Label(s.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -text-muted; -fx-font-style: italic; -fx-font-size: 11px;");

        details.getChildren().addAll(stars, desc);

        card.setOnMouseClicked(e -> {
            boolean isExpanded = details.isVisible();
            details.setVisible(!isExpanded);
            details.setManaged(!isExpanded);
            if (!isExpanded) card.getStyleClass().add("card-expanded");
            else card.getStyleClass().remove("card-expanded");
        });


        card.getChildren().addAll(header, badges, details);
        return card;
    }

}