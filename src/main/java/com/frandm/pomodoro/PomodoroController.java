package com.frandm.pomodoro;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.collections.ObservableList;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.AudioClip;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import javax.xml.crypto.Data;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PomodoroController {

    //region FXML Nodes
    public VBox streakVBox;
    public VBox streakImage;
    public Circle circleMain;
    public TextField tagNameInput;
    public ColorPicker tagColorInput;
    public TextField fuzzySearchInput;
    public VBox fuzzyResultsContainer;
    public Label selectedNameLabel;
    public VBox tagsListContainer;
    public Label taskField; // Campos nuevos
    public Label tagField;  // Campos nuevos
    public Button selectTaskBtn;
    public AreaChart weeklyLineChart;
    public CategoryAxis weeksXAxis;
    public PieChart tagPieChart;

    @FXML private StackPane rootPane, setupPane;
    @FXML private VBox mainContainer, settingsPane, statsContainer, historyContainer, statsPlaceholder;
    @FXML private Label timerLabel, stateLabel, workValLabel, shortValLabel, longValLabel, intervalValLabel, alarmVolumeValLabel, widthSliderValLabel, streakLabel, timeThisWeekLabel, timeLastMonthLabel, tasksLabel, bestDayLabel;
    @FXML private Button startPauseBtn, skipBtn, finishBtn, menuBtn, statsBtn, historyBtn;
    @FXML private Arc progressArc;
    @FXML private Slider workSlider, shortSlider, longSlider, intervalSlider, alarmVolumeSlider, widthSlider;
    @FXML private ToggleButton autoBreakToggle, autoPomoToggle, countBreakTime;
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colDate, colSubject, colTopic, colDescription;
    @FXML private TableColumn<Session, Integer> colDuration;
    @FXML private ColumnConstraints colRightStats, colCenterStats, colLeftStats;
    //endregion

    private final PomodoroEngine engine = new PomodoroEngine();
    private StatsDashboard statsDashboard;
    private boolean isSettingsOpen = false, isSetupOpen = false, isDarkMode = true;
    private TranslateTransition settingsAnim;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime startDate;

    private String filterTag = null;
    private String currentSelectedTag = null;
    private String currentSelectedTask = null;
    private Map<String, List<String>> tagsWithTasksMap = new HashMap<>();
    private Map<String, String> tagColors = new HashMap<>();

    @FXML
    public void initialize() {
        DatabaseHandler.initializeDatabase();
        //DatabaseHandler.generateRandomPomodoros();
        ConfigManager.load(engine);
        refreshDatabaseData();

        if (isDarkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            rootPane.getStyleClass().add("primer-dark");
            rootPane.getStyleClass().remove("primer-light");
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            rootPane.getStyleClass().add("primer-light");
            rootPane.getStyleClass().remove("primer-dark");
        }

        //region config de la tabla
        colDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("tag"));
        colTopic.setCellValueFactory(new PropertyValueFactory<>("task"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("totalMinutes"));

        //endregion

        //region dashboard
        statsDashboard = new StatsDashboard();
        statsPlaceholder.getChildren().clear();
        statsPlaceholder.getChildren().add(statsDashboard);
        //endregion

        //region paneles
        settingsPane.setTranslateX(-600);
        //endregion

        //region settings panel
        setupSlider(workSlider, workValLabel, engine.getWorkMins(), engine::setWorkMins, "");
        setupSlider(shortSlider, shortValLabel, engine.getShortMins(), engine::setShortMins, "");
        setupSlider(longSlider, longValLabel, engine.getLongMins(), engine::setLongMins, "");
        setupSlider(intervalSlider, intervalValLabel, engine.getInterval(), engine::setInterval, "");
        setupSlider(alarmVolumeSlider, alarmVolumeValLabel, engine.getAlarmSoundVolume(), engine::setAlarmSoundVolume, "%");
        setupSlider(widthSlider,widthSliderValLabel,engine.getWidthStats(), engine::setWidthStats, "%");
        colCenterStats.percentWidthProperty().bind(widthSlider.valueProperty());
        colLeftStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));
        colRightStats.percentWidthProperty().bind(widthSlider.valueProperty().multiply(-1).add(100).divide(2));

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoBreakToggle.setText(autoBreakToggle.isSelected() ? "ON" : "OFF");
        autoBreakToggle.selectedProperty().addListener((_, _, isSelected) -> {
            autoBreakToggle.setText(isSelected ? "ON" : "OFF");
            updateEngineSettings();
        });

        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        autoPomoToggle.setText(autoPomoToggle.isSelected() ? "ON" : "OFF");
        autoPomoToggle.selectedProperty().addListener((_, _, isSelected) -> {
            autoPomoToggle.setText(isSelected ? "ON" : "OFF");
            updateEngineSettings();
        });

        countBreakTime.setSelected(engine.isCountBreakTime());
        countBreakTime.setText(countBreakTime.isSelected() ? "ON" : "OFF");
        countBreakTime.selectedProperty().addListener((_, _, isSelected) -> {
            countBreakTime.setText(isSelected ? "ON" : "OFF");
            updateEngineSettings();
        });
        //endregion

        fuzzySearchInput.textProperty().addListener((obs, old, val) -> updateFuzzyResults(val));

        engine.setOnTick(() -> Platform.runLater(() -> {
            timerLabel.setText(engine.getFormattedTime());
            updateProgressCircle();
        }));
        engine.setOnStateChange(() -> Platform.runLater(this::updateUIFromEngine));
        engine.setOnTimerFinished(() -> Platform.runLater(this::playAlarmSound));

        updateEngineSettings();
        updateUIFromEngine();
    }

    //region fuzzy
    private void updateFuzzyResults(String input) {
        fuzzyResultsContainer.getChildren().clear();
        if (input == null || input.trim().isEmpty()) return;

        tagsWithTasksMap.forEach((tag, tasks) -> {
            if (filterTag == null || filterTag.equals(tag)) {
                List<ExtractedResult> matches = FuzzySearch.extractTop(input, tasks, 3);
                for (ExtractedResult match : matches) {
                    if (match.getScore() > 50) {
                        fuzzyResultsContainer.getChildren().add(createResultButton(match.getString(), tag));
                    }
                }
            }
        });

        fuzzyResultsContainer.getChildren().add(new Separator());
        if (filterTag != null) {
            Button createBtn = new Button("+ Create Task: '" + input + "' in " + filterTag);
            createBtn.setMaxWidth(Double.MAX_VALUE);
            createBtn.setOnAction(e -> selectTask(input, filterTag)); //TODO: que lleve a un menu de crear task y quitar el if de arriba
            fuzzyResultsContainer.getChildren().add(createBtn);
        }
    }

    private Button createResultButton(String task, String tag) {
        Button btn = new Button(task + " (" + tag + ")");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        String color = tagColors.getOrDefault(tag, "#4a90e2");
        btn.setStyle("-fx-border-color: " + color + "; -fx-border-width: 0 0 0 4; -fx-background-color: rgba(255,255,255,0.05);");
        btn.setOnAction(e -> selectTask(task, tag));
        return btn;
    }

    private void selectTask(String task, String tag) {
        this.currentSelectedTask = task;
        this.currentSelectedTag = tag;
        selectedNameLabel.setText(tag + " > " + task);
        selectedNameLabel.setStyle("-fx-text-fill: " + tagColors.getOrDefault(tag, "#ffffff") + ";");
        selectTaskBtn.setDisable(false);
    }

    private void refreshTagsList() {
        tagsListContainer.getChildren().clear();
        tagColors.forEach((name, color) -> {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new javafx.geometry.Insets(8));
            if (name.equals(filterTag)) row.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 5;");
            Circle c = new Circle(6, Color.web(color));
            Label l = new Label(name);
            l.setTextFill(Color.WHITE);
            row.getChildren().addAll(c, l);
            row.setOnMouseClicked(e -> {
                filterTag = (name.equals(filterTag)) ? null : name;
                refreshTagsList();
                updateFuzzyResults(fuzzySearchInput.getText());
            });
            tagsListContainer.getChildren().add(row);
        });
    }

    @FXML
    private void handleStartSessionFromSetup() {
        if (currentSelectedTag != null && currentSelectedTask != null) {
            tagField.setText(currentSelectedTag);
            taskField.setText(currentSelectedTask);

            toggleSetup();
        }
    }

    @FXML
    private void handleAddNewTag() {
        String name = tagNameInput.getText().trim();
        if (!name.isEmpty()) {
            String color = "#" + Integer.toHexString(tagColorInput.getValue().hashCode()).substring(0, 6); // TODO: arreglar no funciona los colores
            DatabaseHandler.createTag(name, color);
            refreshDatabaseData();
            refreshTagsList();
            tagNameInput.clear();
        }
    }
    //endregion

    //region handleActions
    @FXML
    private void handleFinish() {
        int mins = engine.getRealMinutesElapsed();

        if (mins >= 1 && currentSelectedTask != null && currentSelectedTag != null) {
            int taskId = DatabaseHandler.getOrCreateTask(currentSelectedTag, tagColors.getOrDefault(currentSelectedTag, "#3498db"), currentSelectedTask);
            DatabaseHandler.saveSession(taskId, "Pomodoro", "", mins, startDate, LocalDateTime.now());
            refreshDatabaseData();
        }

        engine.stop();
        engine.fullReset();
        engine.resetTimeForState(PomodoroEngine.State.MENU);

        currentSelectedTag = null;
        currentSelectedTask = null;
        updateUIFromEngine();
    }

    @FXML private void handleMainAction() {
        if(engine.getCurrentState() == PomodoroEngine.State.WAITING){
            engine.start();
        }else{
            engine.pause();
        }

        if (engine.getCurrentState() == PomodoroEngine.State.MENU){
            if(currentSelectedTag == null || currentSelectedTask == null){
                toggleSetup();
            }else{
                engine.start();
                startDate = LocalDateTime.now();
            }
        }else{
            engine.pause();
        }
        updateUIFromEngine();
    }

    @FXML
    private void handleSkip() { engine.skip(); }

    private void showMainView() {
        Region currentVisible = statsContainer.isVisible() ? statsContainer : historyContainer;
        if (mainContainer.isVisible()) return;
        switchPanels(currentVisible, mainContainer);
    }
    private void showStatsView() {
        if (statsContainer.isVisible()) return;

        ObservableList<Session> data = DatabaseHandler.getAllSessions();
        statsDashboard.updateHeatmap(DatabaseHandler.getMinutesPerDayLastYear());
        updateStatsCards(data);

        Region currentVisible = mainContainer.isVisible() ? mainContainer : historyContainer;
        switchPanels(currentVisible, statsContainer);
    }
    private void showHistoryView() {
        if (historyContainer.isVisible()) return;

        ObservableList<Session> data = DatabaseHandler.getAllSessions();
        sessionsTable.setItems(data);

        Region currentVisible = mainContainer.isVisible() ? mainContainer : statsContainer;
        switchPanels(currentVisible, historyContainer);
    }
    //endregion

    //region setup and settings

    private void setupSlider(Slider s, Label l, int v, java.util.function.Consumer<Integer> a, String t) {
        s.setValue(v); l.setText(v + t);
        s.valueProperty().addListener((o, ov, nv) -> {
            l.setText(nv.intValue() + t);
            a.accept(nv.intValue());
            if (engine.getCurrentState() == PomodoroEngine.State.MENU) timerLabel.setText(engine.getFormattedTime());
        });
    }

    private void updateEngineSettings() {
        engine.updateSettings((int)workSlider.getValue(),
                (int)shortSlider.getValue(),
                (int)longSlider.getValue(),
                (int)intervalSlider.getValue(),
                autoBreakToggle.isSelected(),
                autoPomoToggle.isSelected(),
                countBreakTime.isSelected(),
                (int)alarmVolumeSlider.getValue(),
                (int)widthSlider.getValue());
    }

    private void applyTheme() {
        rootPane.getStyleClass().removeAll("primer-dark", "primer-light");
        if (isDarkMode) { Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet()); rootPane.getStyleClass().add("primer-dark"); }
        else { Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet()); rootPane.getStyleClass().add("primer-light"); }
    }

    @FXML private void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme();
    }

    @FXML private void toggleSetup() {
        if (!isSetupOpen) {
            refreshTagsList();
            selectTaskBtn.setDisable(true);
            fuzzySearchInput.clear();
            fuzzyResultsContainer.getChildren().clear();
            Platform.runLater(() -> fuzzySearchInput.requestFocus());
        }
        isSetupOpen = !isSetupOpen;
        setupPane.setVisible(isSetupOpen);
        setupPane.setManaged(isSetupOpen);
    }

    @FXML private void toggleSettings() {
        if (settingsAnim != null) settingsAnim.stop();
        settingsAnim = new TranslateTransition(Duration.millis(400), settingsPane);
        if (isSettingsOpen) {
            updateEngineSettings();
            ConfigManager.save(engine);
            settingsAnim.setToX(-600);
            settingsAnim.setOnFinished(e -> settingsPane.setVisible(false));
        }
        else { settingsPane.setVisible(true); settingsAnim.setToX(0); }
        settingsAnim.play(); isSettingsOpen = !isSettingsOpen;
    }
    //endregion

    //region UI Updates
    private void updateUIFromEngine() {
        PomodoroEngine.State current = engine.getCurrentState();
        PomodoroEngine.State logical = engine.getLogicalState();
        boolean isMenu = (current == PomodoroEngine.State.MENU);

        if (current == PomodoroEngine.State.MENU) {
            startPauseBtn.setText("START");
        } else {
            startPauseBtn.setText(current == PomodoroEngine.State.WAITING ? "RESUME" : "PAUSE");
        }


        boolean isRunning = (current != PomodoroEngine.State.WAITING && !isMenu);
        skipBtn.setVisible(isRunning);
        skipBtn.setManaged(isRunning);

        boolean hasStarted = (!isMenu);
        finishBtn.setVisible(hasStarted);
        finishBtn.setManaged(hasStarted);

        switch (logical) {
            case WORK -> {
                animateCircleFill(circleMain, "-color-work");
                int session = engine.getSessionCounter() + 1;
                stateLabel.setText(String.format("Pomodoro - #%d", session));
                stateLabel.setStyle("-fx-text-fill: -color-work-secundary;");
                progressArc.setStyle("-fx-stroke: -color-work-secundary;");
                timerLabel.setStyle("-fx-text-fill: -color-work-secundary;");
            }
            case SHORT_BREAK -> {
                animateCircleFill(circleMain, "-color-break");
                stateLabel.setText("Short Break");
                stateLabel.setStyle("-fx-text-fill: -color-break-secundary;");
                progressArc.setStyle("-fx-stroke: -color-break-secundary;");
                timerLabel.setStyle("-fx-text-fill: -color-break-secundary;");
            }
            case LONG_BREAK -> {
                animateCircleFill(circleMain, "-color-long-break");
                stateLabel.setText("Long Break");
                stateLabel.setStyle("-fx-stroke: -color-long-break-secundary;");
                progressArc.setStyle("-fx-stroke: -color-long-break-secundary;");
                timerLabel.setStyle("-fx-stroke: -color-long-break-secundary;");
            }
            case MENU -> {
                animateCircleFill(circleMain, "-color-work");
                stateLabel.setText("Pomodoro");
                stateLabel.setStyle("-fx-text-fill: -color-work-secundary;");
                progressArc.setStyle("-fx-stroke: -color-work-secundary;");
                timerLabel.setStyle("-fx-text-fill: -color-work-secundary;");
            }
            default -> {}
        }
    }

    private void animateCircleFill(Circle circle, String cssVar) {
        Paint currentFill = circle.getFill();
        Color startColor = (currentFill instanceof Color) ? (Color) currentFill : Color.TRANSPARENT;
        circle.setStyle("-fx-fill: " + cssVar + ";");
        circle.applyCss();
        Color targetColor = (circle.getFill() instanceof Color) ? (Color) circle.getFill() : startColor;
        SimpleObjectProperty<Paint> fillProp = new SimpleObjectProperty<>(startColor);
        fillProp.addListener((o, ov, nv) -> circle.setFill(nv));
        new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(fillProp, startColor)), new KeyFrame(Duration.millis(200), new KeyValue(fillProp, targetColor))).play();
    }

    private void updateProgressCircle() {

        double remaining = engine.getSecondsRemaining();
        double total = engine.getTotalSecondsForCurrentState();
        double elapsed = total - remaining;
        double ratio = (total > 0) ? (elapsed/total) : 0;
        double angle = ratio * -360;

        Platform.runLater(() -> progressArc.setLength(angle));
    }
    //endregion

    //region stats
    @FXML
    private void handleNavClick(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();

        menuBtn.getStyleClass().remove("active");
        statsBtn.getStyleClass().remove("active");
        historyBtn.getStyleClass().remove("active");
        clickedBtn.getStyleClass().add("active");

        if (clickedBtn == menuBtn) {
            showMainView();
        } else if (clickedBtn == statsBtn) {
            showStatsView();
        } else if (clickedBtn == historyBtn) {
            showHistoryView();
        }
    }

    private void switchPanels(Region toHide, Region toShow) {
        toShow.setOpacity(0.0);
        toHide.setOpacity(1.0);
        toShow.setVisible(true);
        toShow.setManaged(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toHide);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(_ -> {
            toHide.setVisible(false);
            toHide.setManaged(false);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toShow);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private void playAlarmSound() {
        try {
            URL url = getClass().getResource("sounds/birds.mp3");
            if (url != null) {
                AudioClip c = new AudioClip(url.toExternalForm());
                c.setVolume((double) engine.getAlarmSoundVolume() / 100); c.play();
            }
        } catch (Exception ignored) {

        }
    }

    private void updateStatsCards(ObservableList<Session> sessions) {
        if (sessions == null) return;
        java.time.LocalDate today = java.time.LocalDate.now();

        updateTimeThisWeek(sessions);
        updateTimeLastMonth(sessions);
        calculateStreak(sessions);
        updateBestDay(sessions);
        tasksLabel.setText(String.valueOf(sessions.size()));
        updateSubjectsChart(sessions);
        updateWeeklyChart(sessions);
    }

    private void calculateStreak(ObservableList<Session> s) {
        Set<LocalDate> dates = new HashSet<>(); s.forEach(sess -> dates.add(LocalDate.parse(sess.getStartDate(), DATE_FORMATTER)));
        int streak = 0; LocalDate c = LocalDate.now(); if (!dates.contains(c)) c = c.minusDays(1);
        while (dates.contains(c)) { streak++; c = c.minusDays(1); }
        streakLabel.setText(streak + " Days");
        streakVBox.getStyleClass().removeAll("stat-cardred", "stat-cardbasic");
        streakVBox.getStyleClass().add(streak > 0 ? "stat-cardred" : "stat-cardbasic");
        streakImage.setVisible(streak > 0); streakImage.setManaged(streak > 0);
    }

    private void updateBestDay(ObservableList<Session> s) {
        if (s.isEmpty()) { bestDayLabel.setText("-"); return; }
        String best = s.stream().collect(java.util.stream.Collectors.groupingBy(sess -> LocalDate.parse(sess.getStartDate(), DATE_FORMATTER).getDayOfWeek(), java.util.stream.Collectors.summingInt(Session::getTotalMinutes)))
                .entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(e -> e.getKey().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())).orElse("-");
        bestDayLabel.setText(best.substring(0, 1).toUpperCase() + best.substring(1));
    }

    private void updateTimeThisWeek(ObservableList<Session> s) {
        LocalDate start = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        double mins = s.stream().filter(sess ->
                !LocalDate.parse(sess.getStartDate(),
                DATE_FORMATTER).isBefore(start)).mapToDouble(Session::getTotalMinutes).sum();

        timeThisWeekLabel.setText(String.format("%.1fh", mins / 60));
    }

    private void updateTimeLastMonth(ObservableList<Session> s) {
        LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate end = start.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        double mins = s.stream().filter(sess -> {
            LocalDate d = LocalDate.parse(sess.getStartDate(), DATE_FORMATTER);
            return !d.isBefore(start) && !d.isAfter(end);
        }).mapToDouble(Session::getTotalMinutes).sum();

        timeLastMonthLabel.setText(String.format("%.1fh", mins / 60));
    }

    private void updateSubjectsChart(ObservableList<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            tagPieChart.getData().clear();
            return;
        }

        java.util.Map<String, Integer> timeBySubject = sessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Session::getTag,
                        java.util.stream.Collectors.summingInt(Session::getTotalMinutes)
                ));

        javafx.collections.ObservableList<PieChart.Data> pieData = javafx.collections.FXCollections.observableArrayList();

        timeBySubject.forEach((subject, totalMinutes) -> {
            float hours = (float) totalMinutes / 60;

            String label = String.format("%s (%.1fh)", subject, hours);

            PieChart.Data data = new PieChart.Data(label, hours);
            pieData.add(data);
        });

        tagPieChart.setData(pieData);

        for (PieChart.Data data : tagPieChart.getData()) {
            double sliceValue = data.getPieValue();
            double totalValue = pieData.stream().mapToDouble(PieChart.Data::getPieValue).sum();
            double percent = (sliceValue / totalValue) * 100;

            Tooltip tt = new Tooltip(String.format("%.1f%%\n%s", percent, data.getName()));
            tt.getStyleClass().add("heatmap-tooltip");
            tt.setShowDelay(Duration.millis(75));

            Tooltip.install(data.getNode(), tt);

            data.getNode().setOnMouseEntered(_ -> data.getNode().setStyle("-fx-opacity: 0.75; -fx-cursor: hand;"));
            data.getNode().setOnMouseExited(_ -> data.getNode().setStyle("-fx-opacity: 1.0;"));
        }
    }

    private void updateWeeklyChart(ObservableList<Session> sessions) {
        weeklyLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        java.time.format.DateTimeFormatter dateFormatter =
                java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.getDefault());

        java.time.format.DateTimeFormatter labelFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 11; i >= 0; i--) {
            LocalDate endOfWeek = LocalDate.now().minusWeeks(i).with(java.time.DayOfWeek.SUNDAY);
            LocalDate startOfWeek = endOfWeek.minusDays(6);

            double totalMins = sessions.stream()
                    .filter(s -> {
                        LocalDate d = LocalDate.parse(s.getStartDate(), DATE_FORMATTER);
                        return !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek);
                    })
                    .mapToDouble(Session::getTotalMinutes)
                    .sum();

            String label = startOfWeek.format(labelFormatter);
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, totalMins/60);

            dataPoint.setExtraValue(new LocalDate[]{startOfWeek, endOfWeek});
            series.getData().add(dataPoint);
        }

        weeklyLineChart.getData().add(series);

        for (XYChart.Data<String, Number> data : series.getData()) {
            LocalDate[] dates = (LocalDate[]) data.getExtraValue();
            LocalDate start = dates[0];
            LocalDate end = dates[1];
            int totalMinutes = (int)(data.getYValue().doubleValue()*60);
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;

            Tooltip tooltip = new Tooltip(String.format("%s - %s\n %dh %dm", start.format(dateFormatter), end.format(dateFormatter), hours, minutes));

            tooltip.setShowDelay(Duration.millis(50));
            tooltip.getStyleClass().add("heatmap-tooltip");

            Tooltip.install(data.getNode(), tooltip);

            data.getNode().setOnMouseEntered(e -> {
                data.getNode().setScaleX(1.5);
                data.getNode().setScaleY(1.5);
                data.getNode().setCursor(javafx.scene.Cursor.HAND);
            });

            data.getNode().setOnMouseExited(e -> {
                data.getNode().setScaleX(1.0);
                data.getNode().setScaleY(1.0);
            });
        }
    }

    private void refreshDatabaseData() {
        this.tagsWithTasksMap = DatabaseHandler.getTagsWithTasksMap();
        this.tagColors = DatabaseHandler.getTagColors();
    }

    @FXML
    private void handleResetTimeSettings() {
        engine.resetToDefaults();

        workSlider.setValue(engine.getWorkMins());
        shortSlider.setValue(engine.getShortMins());
        longSlider.setValue(engine.getLongMins());
        intervalSlider.setValue(engine.getInterval());

        autoBreakToggle.setSelected(engine.isAutoStartBreaks());
        autoPomoToggle.setSelected(engine.isAutoStartPomo());
        countBreakTime.setSelected(engine.isCountBreakTime());

        updateEngineSettings();
        updateUIFromEngine();
        ConfigManager.save(engine);
    }
    //endregion
}