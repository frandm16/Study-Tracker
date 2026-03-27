package com.frandm.studytracker.ui.util;

import com.frandm.studytracker.controllers.PomodoroController;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class UIManager {

    private static final Duration SLIDE_DURATION = Duration.millis(300);
    private static final Duration FADE_DURATION = Duration.millis(250);

    public void switchPanels(Region toHide, Region toShow, int direction) {
        if (toHide == null || toShow == null || toHide == toShow) return;

        Platform.runLater(() -> {
            double width = toHide.getParent() instanceof Region p ? p.getWidth() : toHide.getWidth();
            if (width <= 0) width = 800;
            double offset = width * direction;

            toHide.setCache(true);
            toHide.setCacheHint(CacheHint.SPEED);
            toShow.setCache(true);
            toShow.setCacheHint(CacheHint.SPEED);

            TranslateTransition slideOut = new TranslateTransition(SLIDE_DURATION, toHide);
            slideOut.setByX(-offset * 0.3);
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            FadeTransition fadeOut = new FadeTransition(FADE_DURATION, toHide);
            fadeOut.setToValue(0.0);

            ParallelTransition exitTransition = new ParallelTransition(slideOut, fadeOut);

            exitTransition.setOnFinished(e -> {
                toHide.setVisible(false);
                toHide.setManaged(false);
                toHide.setTranslateX(0);

                toShow.setOpacity(0.0);
                toShow.setTranslateX(offset * 0.3);
                toShow.setVisible(true);
                toShow.setManaged(true);
            });

            TranslateTransition slideIn = new TranslateTransition(SLIDE_DURATION, toShow);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            FadeTransition fadeIn = new FadeTransition(FADE_DURATION, toShow);
            fadeIn.setToValue(1.0);

            ParallelTransition enterTransition = new ParallelTransition(slideIn, fadeIn);

            SequentialTransition fullTransition = new SequentialTransition(exitTransition, enterTransition);

            fullTransition.setOnFinished(e -> {
                toHide.setOpacity(1.0);
                toHide.setCache(false);
                toShow.setCache(false);
                toShow.setMouseTransparent(false);
            });

            toHide.setMouseTransparent(true);
            fullTransition.play();
        });
    }

    public void animateCircleColor(Circle circle, String cssVar) {
        Paint currentFill = circle.getFill();
        Color startColor = (currentFill instanceof Color) ? (Color) currentFill : Color.TRANSPARENT;
        circle.setStyle("-fx-fill: " + cssVar + ";");
        circle.applyCss();
        Color targetColor = (circle.getFill() instanceof Color) ? (Color) circle.getFill() : startColor;
        SimpleObjectProperty<Paint> fillProp = new SimpleObjectProperty<>(startColor);
        fillProp.addListener((o, ov, nv) -> circle.setFill(nv));
        new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(fillProp, startColor)), new KeyFrame(Duration.millis(200), new KeyValue(fillProp, targetColor))).play();
    }

    public void updateActiveBadge(VBox container, String tag, String task, String color, PomodoroController controller) {
        container.getChildren().clear();
        Button tagBtn = new Button(tag);
        tagBtn.setOnAction(e -> controller.toggleSetup());
        tagBtn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-radius: 12; " +
                        "-fx-padding: 2 10; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;"
        );

        if (task != null) {
            Button taskBtn = new Button(task);
            taskBtn.setOnAction(e -> controller.toggleSetup());
            taskBtn.getStyleClass().add("task-badge");
            container.getChildren().addAll(tagBtn, taskBtn);
        } else {
            container.getChildren().add(tagBtn);
        }
    }
}
