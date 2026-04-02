package com.frandm.studytracker;

import com.frandm.studytracker.controllers.TrackerController;
import com.frandm.studytracker.core.NotificationManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import xss.it.nfx.NfxStage;
import java.io.IOException;

public class MainStage extends NfxStage {
    private final FXMLLoader loader;

    public MainStage() throws IOException {
        this.loader = new FXMLLoader(getClass().getResource("/com/frandm/studytracker/fxml/main_view.fxml"));
        Parent root = loader.load();
        TrackerController controller = loader.getController();

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        this.setScene(scene);

        this.setCloseControl(controller.closeBtn);
        this.setMaxControl(controller.maxBtn);
        this.setMinControl(controller.minBtn);

        controller.closeBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (controller.isTimerActive()) {
                NotificationManager.show(
                        "Close blocked",
                        "You must finish your current session to close the app.",
                        NotificationManager.NotificationType.WARNING
                );
                event.consume();
            }
        });

        this.windowStateProperty().addListener((_, _, newState) -> {
            FontIcon maxIcon = (FontIcon) controller.maxBtn.getGraphic();
            if (newState == xss.it.nfx.WindowState.MAXIMIZED) {
                maxIcon.setIconLiteral("ri-window-restore");
            } else {
                maxIcon.setIconLiteral("ri-window-maximize");
            }
        });
    }

    public FXMLLoader getLoader() {
        return loader;
    }

    @Override
    protected double getTitleBarHeight() {
        return 40.0;
    }
}