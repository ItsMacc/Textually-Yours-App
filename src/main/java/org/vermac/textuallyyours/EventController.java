package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class EventController {
    private AppController appController;
    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("h:mm a");

    @FXML
    private StackPane root;
    @FXML
    private TextField eventName;
    @FXML
    private DatePicker date;
    @FXML
    private TextField time;
    @FXML
    private Button send;

    public void initialize() {
        String color = AppStateManager.fetchProperty("backgroundColor");
        root.setStyle("-fx-background-color: " + color);
    }

    public void setMainController(AppController appController) {
        this.appController = appController;
    }

    public void sendInvite(ActionEvent event) {
        if (isValid()) {
            // Send event invite with ZonedDateTime
            ZonedDateTime zonedDateTime = createZonedDateTime(time.getText());
            appController.sendEventInvite(eventName.getText(), zonedDateTime);

            Stage currentStage =
                    (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();
        } else {
            showValidationAlert();
        }
    }

    /**
     * Validates the event input fields.
     * @return true if all fields are valid, false otherwise.
     */
    public boolean isValid() {
        // Check if event name, description, and time fields are not empty
        if (eventName.getText().trim().isEmpty() ||
                time.getText().trim().isEmpty()) {
            return false;
        }

        // Check if date is selected
        if (date.getValue() == null) {
            return false;
        }

        // Validate time format (e.g., "6:00 PM")
        try {
            // Try parsing the time
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            timeFormatter.parse(time.getText());
        } catch (DateTimeParseException e) {
            return false;
        }

        // All validations passed
        return true;
    }

    /**
     * Converts the input date and time into ZonedDateTime
     *
     * @param timeString The input time in 'h:mm a' format
     * @return The ZonedDateTime representing the event date and time
     */
    private ZonedDateTime createZonedDateTime(String timeString) {
        LocalDate eventDate = date.getValue();
        LocalTime eventTime = LocalTime.parse(timeString, timeFormatter);

        // Combine date and time
        LocalDateTime localDateTime = LocalDateTime.of(eventDate, eventTime);

        ZoneId systemZone = ZoneId.systemDefault();

        return ZonedDateTime.of(localDateTime, systemZone);
    }

    /**
     * Displays an alert when validation fails.
     */
    private void showValidationAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Input");
        alert.setHeaderText("Please check your input.");
        alert.setContentText("""
                Make sure all fields are filled out correctly:
                - Event name cannot be empty.
                - Date must be selected.
                - Time must be in 'h:mm a' format (e.g., 6:00 pm).""");

        alert.showAndWait();
    }
}
