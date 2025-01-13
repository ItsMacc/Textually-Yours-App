package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

public class EventController {
    private AppController appController;
    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("hh:mm a");

    @FXML
    private VBox root;
    @FXML
    private ImageView bgImage;
    @FXML
    public TextField eventName;
    @FXML
    public TextArea description;
    @FXML
    public DatePicker date;
    @FXML
    public TextField time;

    public void initialize() {
        String color = AppStateManager.fetchProperty("backgroundColor");
        root.setStyle("-fx-background-color: " + color);
    }

    public void setMainController(AppController appController) {
        this.appController = appController;
    }

    public void sendInvite() {
        if (isValid()) {
            // Send event invite with ZonedDateTime
            System.out.println(date.getValue().toString());
            System.out.println(time.getText());
            ZonedDateTime zonedDateTime = createZonedDateTime(date.getValue().toString(), time.getText());
            appController.sendEventInvite(eventName.getText(), description.getText(), zonedDateTime);
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
                description.getText().trim().isEmpty() ||
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
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
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
     * @param dateString The selected date as a string
     * @param timeString The input time in 'h:mm a' format
     * @return The ZonedDateTime representing the event date and time
     */
    private ZonedDateTime createZonedDateTime(String dateString, String timeString) {
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
                - Event name and description cannot be empty.
                - Date must be selected.
                - Time must be in 'h:mm a' format (e.g., 6:00 pm).""");

        alert.showAndWait();
    }
}
