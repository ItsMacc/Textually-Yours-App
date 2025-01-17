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
    private ColorPicker bubble;
    @FXML
    private Button save;
    @FXML
    private Button reset;

    public void initialize() {
        String savedColor = AppStateManager.fetchProperty("backgroundColor");
        String savedBubble = AppStateManager.fetchProperty("bubbleColor");

        color.setValue(Color.valueOf(savedColor));
        bubble.setValue(Color.valueOf(savedBubble));

        color.setOnAction(event -> updateColorPreview());
        bubble.setOnAction(event -> updateBubblePreview());
    }


    public void setMainController(AppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    public void saveSettings(ActionEvent event) {
        AppStateManager.updateProperty("backgroundColor", "#" + color.getValue().toString().substring(2));

        if (bubble.getValue() == null) {
            AppStateManager.updateProperty("bubbleColor", "transparent");
        } else {
            AppStateManager.updateProperty("bubbleColor", "#" + bubble.getValue().toString().substring(2));
        }

        Stage currentStage =
                (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.close();

        mainAppController.applySettings();
    }

    public void resetSettings() {
        AppStateManager.updateProperty("backgroundColor", "#a459cdff");
        color.setValue(Color.valueOf("#a459cdff"));

        AppStateManager.updateProperty("bubbleColor", "transparent");
        bubble.setValue(Color.TRANSPARENT);

        mainAppController.applySettings();
    }

    private void updateColorPreview() {
        Color newColor = color.getValue();
        mainAppController.updateBackgroundColor(newColor);
    }

    private void updateBubblePreview() {
        Color newBubble = bubble.getValue();
        mainAppController.updateBubbleColor(newBubble);
    }
}
