package org.vermac.textuallyyoursinstaller;

import com.AppState.io.AppStateManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.vermac.textuallyyours.AppController;

import java.io.IOException;
import java.util.Optional;

/**
 * InstallerController class for managing the interaction between the FXML layout and the application logic.
 * Handles user input for creating or joining a session, validates input, and displays appropriate screens.
 *
 * @author Mac
 * @version 1.0
 */
public class InstallerController {
    private AppController appController;

    public static String name, admin, otherEmail;

    @FXML
    private Button createButton;
    @FXML
    private Button joinButton;
    @FXML
    private VBox init;
    @FXML
    private VBox root;
    @FXML
    private TextField username;
    @FXML
    private TextField email;
    @FXML
    private Label alreadyInstalled;

    /**
     * Initializes the controller.
     *
     */
    public void initialize() {}

    /**
     * Displays the 'Create Form' when the 'Create' button is clicked.
     * If the input is valid, asks for user confirmation before proceeding.
     */
    public void showCreateForm() {

        if (isValidInput()) {  // Check if input is valid
            // Ask for user confirmation
            if (showAlert("Create Form", "Are you sure you want to create?", Alert.AlertType.CONFIRMATION)) {

                // Initialize app for admin
                name = username.getText();
                otherEmail = email.getText();
                admin = "true";

                showChatInfoScreen();  // Proceed to the Chat Info Screen
            }
        }
    }

    /**
     * Displays the 'Join Form' when the 'Join' button is clicked.
     * If the input is valid, asks for user confirmation before proceeding.
     */
    public void showJoinForm() {
        if (isValidInput()) {  // Check if input is valid
            // Ask for user confirmation
            if (showAlert("Join Form", "Are you sure you want to join?", Alert.AlertType.CONFIRMATION)) {

                // Initialize the app for client
                name = username.getText();
                otherEmail = email.getText();
                admin = "false";

                // Load the Join screen (join-info.fxml)
                showJoinInfoScreen();  // Proceed to the Join Info Screen
            }
        }
    }

    /**
     * Validates the user input for both username and email fields.
     * If either field is empty, it shows an error alert.
     *
     * @return true if both username and email are filled, false otherwise.
     */
    private boolean isValidInput() {
        // Check if both username and email are not empty
        if (username.getText().isEmpty() || email.getText().isEmpty()) {
            // Show an alert if either field is empty
            showAlert("Input Error", "Please fill in both fields (Username and Email).", Alert.AlertType.ERROR);
            return false;
        }
        return true;
    }

    /**
     * Displays an alert with a given title, message, and alert type.
     * If the alert type is CONFIRMATION, it adds "Yes" and "No" options.
     *
     * @param title The title of the alert.
     * @param message The message to be displayed in the alert.
     * @param alertType The type of alert (ERROR, INFORMATION, CONFIRMATION).
     * @return true if the user pressed "Yes", false if "No" or the alert was closed.
     */
    private boolean showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header
        alert.setContentText(message);

        // If the alert type is CONFIRMATION, add "Yes" and "No" buttons
        if (alertType == Alert.AlertType.CONFIRMATION) {
            ButtonType confirmButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            ButtonType cancelButton = new ButtonType("No", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(confirmButton, cancelButton);
        }

        // Show the alert and wait for the user's response
        Optional<ButtonType> result = alert.showAndWait();

        // Check the result and determine what button was pressed
        if (result.isPresent()) {
            ButtonType pressedButton = result.get();
            return pressedButton.getButtonData() == ButtonBar.ButtonData.YES;
        }

        return false;
    }

    /**
     * Loads and displays the Chat Information screen (chat-info.fxml) after the user creates a session.
     */
    private void showChatInfoScreen() {
        try {
            // Load the new FXML file (chat-info.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org.vermac.textuallyyoursinstaller/chat-info.fxml"));

            Scene scene = new Scene(loader.load(), 480, 480);

            // Create and show a new stage (window)
            Stage stage = new Stage();
            stage.setTitle("Room Information");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            showAlert("Error", "Failed to load Chat Information screen.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Loads and displays the Join Information screen (join-info.fxml) after the user decides to join a session.
     */
    private void showJoinInfoScreen() {
        try {
            // Load the new FXML file for the join screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org.vermac.textuallyyoursinstaller/join-info.fxml"));

            Scene scene = new Scene(loader.load(), 480, 480);

            // Create and show a new stage (window)
            Stage stage = new Stage();
            stage.setTitle("Join Room");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            showAlert("Error", "Failed to load Join Room screen.", Alert.AlertType.ERROR);
        }
    }

    public void setAppController(AppController appController){
        this.appController = appController;
    }
}
