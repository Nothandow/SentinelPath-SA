package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.MainController;

/*
 * Entry point for SentinelPath SA.
 * Launches JavaFX, creates the main window, loads MainController.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController();

        // Window sized to match the screenshot layout
        Scene scene = new Scene(controller.getRoot(), 1100, 700);
        stage.setTitle("SentinelPath SA");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
