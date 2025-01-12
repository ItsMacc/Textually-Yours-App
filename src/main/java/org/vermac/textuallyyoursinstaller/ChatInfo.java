package org.vermac.textuallyyoursinstaller;

import com.AppState.io.AppStateManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Collections;
import java.util.Random;

/**
 * ChatInfo class for managing the server-side logic of a chat room.
 * It handles the creation of a server, waiting for a client to join,
 * exchanging information with the client, and managing the connection.
 *
 * @author Mac
 * @version 1.0
 */
public class ChatInfo {

    // Constants
    private static final int WAIT_TIME_MS = 15*60*1000;
    private static final int port = new Random().nextInt(10000, 65535); // Random port selection
    private static final String IP = getLocalIPAddress(); // Local IP address

    @FXML
    private Label roomCode;  // Label for displaying the room code
    @FXML
    private Label address;   // Label for displaying the server address
    @FXML
    private Label exit;      // Label for showing exit message

    /**
     * Initializes the server by starting a new thread that runs the server logic.
     */
    public void initialize() {
        new Thread(this::startServer).start();  // Start the server in a separate thread
    }

    /**
     * Starts the server on a random port and waits for a client to connect.
     * Once a client connects, it establishes communication and handles the exchange of data.
     */
    public void startServer() {
        setRoomCode(String.valueOf(port));    // Set the room code to the port number
        setAddress(getLocalIPAddress());      // Set the server address to the local IP

        try (ServerSocket serverSocket = new ServerSocket(port, 0,
                InetAddress.getByName("0.0.0.0"))) {
            serverSocket.setSoTimeout(WAIT_TIME_MS);
            Socket client = serverSocket.accept();

            AppStateManager.initializeApp();

            // Update the application state with room details
            AppStateManager.updateProperty("username", InstallerController.name);
            AppStateManager.updateProperty("admin", InstallerController.admin);
            AppStateManager.updateProperty("roomKey", String.valueOf(port));
            AppStateManager.updateProperty("otherUserEmail", InstallerController.otherEmail);
            AppStateManager.updateProperty("serverIP", IP);

            handleCommunication(client);
            exit.setVisible(true);

            client.close();
            serverSocket.close();
        } catch (IOException ignored) {
            System.out.println(ignored.getMessage());
        }
    }

    /**
     * Retrieves the local IP address of the machine running the server.
     *
     * @return The local IP address as a String, or null if unable to retrieve it.
     */
    private static String getLocalIPAddress() {
        try {
            for (NetworkInterface networkInterface :
                    Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                for (InetAddress address :
                        Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address) {
                        return address.getHostAddress();  // Return the local IPv4 address
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Something went wrong: " + e.getMessage());
        }
        return null;  // Return null if unable to retrieve the IP address
    }

    /**
     * Handles the communication with the connected client by exchanging
     * usernames and other necessary data.
     *
     * @param socket The socket representing the connection with the client.
     * @throws IOException If an I/O error occurs during communication.
     */
    private static void handleCommunication(Socket socket) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

        String myUsername = AppStateManager.fetchProperty("username");  // Get current user's username
        String otherUsername;
        String otherUserID;

        // Send important data (username and user ID) to the client
        output.println(myUsername);
        output.println(AppStateManager.getUserID());
        output.println(port);
        output.println(IP);

        // Receive the other user's username and user ID
        otherUsername = input.readLine();
        otherUserID = input.readLine();

        // Update application state with received information
        AppStateManager.updateProperty("otherUser", otherUsername);
        AppStateManager.updateProperty("otherUserID", otherUserID);

        // Mark the app as initialized
        AppStateManager.updateProperty("initialised", "true");
    }

    /**
     * Sets the room code in the roomCode label.
     *
     * @param code The room code to display.
     */
    public void setRoomCode(String code) {
        roomCode.setText(code);
    }

    /**
     * Sets the server address in the address label.
     *
     * @param addressValue The server's IP address to display.
     */
    public void setAddress(String addressValue) {
        address.setText(addressValue);
    }
}
