package org.vermac.textuallyyoursinstaller;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.vermac.textuallyyours.AppLauncher;

public class InstallerApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org.vermac.textuallyyoursinstaller/setup.fxml"));
        VBox root = loader.load();
        Scene scene = new Scene(root,640,640);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Textually Yours Installer");

        primaryStage.setOnCloseRequest(windowEvent -> {
            Platform.exit();
        });
        primaryStage.show();
    }
}
