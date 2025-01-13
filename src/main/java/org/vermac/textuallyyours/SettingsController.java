package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class SettingsController {
    private AppController mainAppController;

    @FXML
    private ColorPicker color;
    @FXML
    private Label allowLabel;
    @FXML
    private RadioButton dynamicTypingEnabled;
    @FXML
    private RadioButton allowUser;
    @FXML
    private Button save;
    @FXML
    private Button reset;

    public void initialize() {
        String savedColor = AppStateManager.fetchProperty("backgroundColor");
        String otherUser = AppStateManager.fetchProperty("otherUser");

        boolean dynamicTyping = Boolean.parseBoolean(AppStateManager.fetchProperty("dynamicTypingEnabled"));
        boolean allow = Boolean.parseBoolean(AppStateManager.fetchProperty("allowUser"));

        allowLabel.setText("Allow " + otherUser + " to change background");

        color.setValue(Color.valueOf(savedColor));
        dynamicTypingEnabled.setSelected(dynamicTyping);
        allowUser.setSelected(allow);

        color.setOnAction(event -> updateColorPreview());
        dynamicTypingEnabled.setOnAction(event -> updateDynamicTypingPreview());
        allowUser.setOnAction(event -> askUpdateUserBackground());
    }

    public void setMainController(AppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    public void saveSettings(ActionEvent event) {
        AppStateManager.updateProperty("backgroundColor", "#" + color.getValue().toString().substring(2));
        AppStateManager.updateProperty("dynamicTypingEnabled", String.valueOf(dynamicTypingEnabled.isSelected()));
        AppStateManager.updateProperty("allowUser", String.valueOf(allowUser.isSelected()));

        Stage currentStage =
                (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.close();
    }

    public void resetSettings() {
        String prevSavedColor = AppStateManager.fetchProperty("backgroundColor");
        boolean prevDynamicTyping = Boolean.parseBoolean(AppStateManager.fetchProperty("dynamicTypingEnabled"));
        boolean prevAllow = Boolean.parseBoolean(AppStateManager.fetchProperty("allowUser"));

        color.setValue(Color.valueOf(prevSavedColor));
        dynamicTypingEnabled.setSelected(prevDynamicTyping);
        allowUser.setSelected(prevAllow);

        mainAppController.applySettings();
    }

    private void updateColorPreview() {
        Color newColor = color.getValue();
        mainAppController.updateBackgroundColor(newColor);
    }

    private void updateDynamicTypingPreview() {
        mainAppController.updateDynamicTyping(dynamicTypingEnabled.isSelected());
    }

    private void askUpdateUserBackground() {
        mainAppController.askUpdateUserBackground();
    }
}
