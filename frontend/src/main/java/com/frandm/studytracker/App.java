package com.frandm.studytracker;

import atlantafx.base.theme.PrimerDark;
import com.frandm.studytracker.controllers.TrackerController;
import com.frandm.studytracker.core.NotificationManager;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import java.net.URL;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Font.loadFont(getClass().getResourceAsStream("/com/frandm/studytracker/fonts/SF-Pro-Display-Regular.otf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/com/frandm/studytracker/fonts/Excalifont-Regular.otf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/com/frandm/studytracker/fonts/SpaceGrotesk-Regular.ttf"), 12);

        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        Stage finalStage;
        Parent root;
        TrackerController controller;

        if (isWindows) {
            MainStage mainStage = new MainStage();
            finalStage = mainStage;
            root = mainStage.getScene().getRoot();
            controller = mainStage.getLoader().getController();
        } else {
            finalStage = stage;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frandm/studytracker/fxml/main_view.fxml"));
            root = loader.load();
            controller = loader.getController();
            controller.titleBar.setVisible(false);
            controller.titleBar.setManaged(false);
            finalStage.setScene(new Scene(root));
        }

        finalStage.setOnCloseRequest(event -> {
            if (controller != null && controller.isTimerActive()) {
                NotificationManager.show(
                        "Close blocked",
                        "You must finish your current session to close the app.",
                        NotificationManager.NotificationType.WARNING
                );
                event.consume();
            } else {
                Platform.exit();
                System.exit(0);
            }
        });

        if (!isWindows && controller != null && controller.closeBtn != null) {
            controller.closeBtn.setOnAction(_ -> {
                finalStage.fireEvent(new WindowEvent(finalStage, WindowEvent.WINDOW_CLOSE_REQUEST));
            });
        }

        Scene scene = finalStage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            finalStage.setScene(scene);
        }

        scene.setFill(Color.TRANSPARENT);
        String mainStyles = Objects.requireNonNull(getClass().getResource("/com/frandm/studytracker/css/styles.css")).toExternalForm();
        scene.getStylesheets().add(mainStyles);

        finalStage.setTitle("Study Tracker");
        finalStage.setResizable(true);

        if (controller != null && controller.titleBar != null) {
            finalStage.fullScreenProperty().addListener((_, _, isFullScreen) -> {
                controller.titleBar.setVisible(!isFullScreen);
                controller.titleBar.setManaged(!isFullScreen);
            });
        }

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.F11.equals(event.getCode())) {
                finalStage.setFullScreen(!finalStage.isFullScreen());
            }
        });

        URL iconUrl = getClass().getResource("/com/frandm/studytracker/images/SZlogo.png");

        if (iconUrl != null) {
            finalStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        finalStage.show();

        Platform.runLater(() -> {
            try {
                finalStage.setMaximized(true);
            } catch (Exception ignored) {
            }

            CSSFX.start();

            if (root instanceof Pane pane && !pane.getChildren().isEmpty()) {
                javafx.scene.Node content = pane.getChildren().getFirst();
                content.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(800), content);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }
        });
    }

    public static void main() {
        launch();
    }
}