package org.vermac.textuallyyoursinstaller;

import com.AppState.io.AppStateManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


/**
 * JoinInfo class is responsible for managing the joining process for a chat room.
 * It allows users to input a room code and server address to join an existing room.
 *
 * @author Mac
 * @version 1.0
 */
public class JoinInfo {

    @FXML
    private TextField roomCode;
    @FXML
    private TextField address;
    @FXML
    private Label exit;

    // This method will be called when the "Join Room" button is clicked
    public void joinRoom() {
        String roomCodeText = roomCode.getText().trim();
        String serverAddress = address.getText().trim();

        // Validate the inputs
        if (roomCodeText.isEmpty() || serverAddress.isEmpty()) {
            showAlert("Input Error", "Both Room Code and Address are required.");
            return;
        }

        try {
            // Convert the room code to an integer (port)
            int roomCodeInt = Integer.parseInt(roomCodeText);
            // Try to connect to the server
            connectToServer(serverAddress, roomCodeInt);
            exit.setVisible(true);
        } catch (NumberFormatException e) {
            showAlert("Invalid Room Code", "Room Code must be a valid number (port).");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            showAlert("Connection Error", "Failed to connect to the server. Please check the server address and room code.");
        }
    }

    // This method will attempt to connect to the server using the room code (port) and server address (IP)
    private void connectToServer(String serverAddress, int roomCode) throws IOException {

        try (Socket socket =
                     new Socket(serverAddress, roomCode)) {

            AppStateManager.initializeApp();

            AppStateManager.updateProperty("username", InstallerController.name);
            AppStateManager.updateProperty("admin", InstallerController.admin);
            AppStateManager.updateProperty("roomKey", String.valueOf(roomCode));
            AppStateManager.updateProperty("otherUserEmail", InstallerController.otherEmail);
            AppStateManager.updateProperty("serverIP", serverAddress);

            System.out.println("Successfully joined the room!");

            // Handle communication once the connection is established (you can reuse the handleCommunication method from earlier)
            handleCommunication(socket);
            exit.setVisible(true);
        }
    }

    // Method to handle communication with the server
    private static void handleCommunication(Socket socket) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

        String myUsername = AppStateManager.fetchProperty("username");

        String otherUsername;
        String otherUserID;

        // Send important data to other user
        output.println(myUsername);
        output.println(AppStateManager.getUserID());

        otherUsername = input.readLine();
        otherUserID = input.readLine();
        String port = input.readLine();
        String IP = input.readLine();

        AppStateManager.updateProperty("roomKey", port);
        AppStateManager.updateProperty("serverIP", IP);
        AppStateManager.updateProperty("otherUser", otherUsername);
        AppStateManager.updateProperty("otherUserID", otherUserID);

        AppStateManager.updateProperty("initialised", "true");
    }

    // Helper method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
