package rebelalliance.smartcash;

import javafx.application.Application;
import javafx.stage.Stage;
import rebelalliance.smartcash.scene.SCScene;

public class SmartCash extends Application {
    @Override
    public void start(Stage stage) {
        // Window setup.
        stage.setTitle("SmartCash");
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.setResizable(false);

        // Show.
        // TODO: Change default scene.
        SceneManager sceneManager = new SceneManager(stage);
        sceneManager.setScene(SCScene.REGISTER);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
