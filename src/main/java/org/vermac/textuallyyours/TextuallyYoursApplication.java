package org.vermac.textuallyyours;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class TextuallyYoursApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root =
                FXMLLoader.load(Objects.requireNonNull(getClass().getResource("textually-yours" +
                ".fxml")));
        Scene scene = new Scene(root, 900, 800);

        stage.setTitle("Textually Yours");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setOnCloseRequest(windowEvent -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

}