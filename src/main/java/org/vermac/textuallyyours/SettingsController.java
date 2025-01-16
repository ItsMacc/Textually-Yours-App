package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class SettingsController {
    private AppController mainAppController;

    @FXML
    private ColorPicker color;
    @FXML
    private Button save;
    @FXML
    private Button reset;

    public void initialize() {
        String savedColor = AppStateManager.fetchProperty("backgroundColor");

        color.setValue(Color.valueOf(savedColor));

        color.setOnAction(event -> updateColorPreview());
    }

    public void setMainController(AppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    public void saveSettings(ActionEvent event) {
        AppStateManager.updateProperty("backgroundColor", "#" + color.getValue().toString().substring(2));

        Stage currentStage =
                (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.close();
    }

    public void resetSettings() {
        String prevSavedColor = AppStateManager.fetchProperty("backgroundColor");

        color.setValue(Color.valueOf(prevSavedColor));
        mainAppController.applySettings();
    }

    private void updateColorPreview() {
        Color newColor = color.getValue();
        mainAppController.updateBackgroundColor(newColor);
    }
}
