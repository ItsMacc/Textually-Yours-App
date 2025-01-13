package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import notification.NotificationSender;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppController {

    // Instance variables
    private static final int WAIT_TIME_MS = 5 * 60 * 1000; // 5 seconds
    private static final ExecutorService executor =
            Executors.newCachedThreadPool();

    private BufferedReader input;
    private PrintWriter output;
    private Socket socket;
    private boolean hasSentNotification = false;

    // FXML resources
    @FXML
    public StackPane window;
    @FXML
    private BorderPane chatScreen;
    @FXML
    private VBox waitingScreen;
    @FXML
    private VBox exitScreen;
    @FXML
    private VBox notInstalledScreen;
    @FXML
    private VBox addressScreen;
    @FXML
    private TextField message;
    @FXML
    private VBox container;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Label dynamicLabel;
    @FXML
    private Button changeBtn;

    // Start the application
    public void initialize() {
        AppStateManager.initializeApp();
        applySettings();

        showWaitingScreen();
        showChatScreen();
//        if (AppStateManager.fetchProperty("initialised").equals("true")) {
//
//            // Initialize the app and apply saved/default settings
//            AppStateManager.initializeApp();
//            applySettings();
//
//            showWaitingScreen();
//
//            int port =
//                    Integer.parseInt(AppStateManager.fetchProperty("roomKey"));
//            String IP =
//                    AppStateManager.fetchProperty("serverIP");
//            boolean isAdmin =
//                    Boolean.parseBoolean(AppStateManager.fetchProperty("admin"));
//
//            List<Message> messageList = AppStateManager.loadMessages();
//
//            for (Message m : messageList) {
//                String sender = m.getSender();
//                String content = m.getContent();
//                addMessageToContainer(content, sender.equals(AppStateManager.getUserID()));
//            }
//
//            if (isAdmin) {
//                startServer(port);
//            } else {
//                startClientConnection(IP, port);
//            }
//
//            // Listen for messages to display dynamically
//            message.textProperty().addListener((observableValue,
//                                                oldValue,
//                                                newValue) -> {
//                if (!newValue.trim().isEmpty()) {
//                    output.println("TYPING: " + newValue);
//                } else {
//                    output.println("STOP");
//                }
//            });
//        }
//        else {
//            window.setStyle("-fx-background-color: #a459cd");
//            notInstalledScreen.setVisible(true);
//        }
    }

    // Handle the text message by adding it to the screen
    public void handleText() {
        if (message.getText() != null && !message.getText().trim().isEmpty()) {
            startSendingThread();
            AppStateManager.saveMessage(
                    AppStateManager.getUserID(),
                    AppStateManager.fetchProperty("otherUserID"),
                    message.getText());
            addMessageToContainer(message.getText(), true, false);
            message.clear();
        }
    }

    public void handleSettings(){
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("settings.fxml"));

        try {
            Scene scene = new Scene(fxmlLoader.load(), 550, 350);
            Stage stage = new Stage();

            SettingsController settingsController = fxmlLoader.getController();
            settingsController.setMainController(this);

            stage.setTitle("Settings");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            stage.setOnCloseRequest(windowEvent -> applySettings());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String savedColor = AppStateManager.fetchProperty(
                "backgroundColor");
        boolean dynamicTyping =
                Boolean.parseBoolean(AppStateManager.fetchProperty("dynamicTypingEnabled"));

        window.setStyle("-fx-background-color: " + savedColor);
        dynamicLabel.setVisible(dynamicTyping);
    }

    public void handleEvent() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("event.fxml"));

        try {
            Scene scene = new Scene(fxmlLoader.load(), 550, 400);
            Stage stage = new Stage();

            EventController eventController = fxmlLoader.getController();
            eventController.setMainController(this);

            stage.setTitle("Plan An Event");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            stage.setOnCloseRequest(windowEvent -> applySettings());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleAddress(){
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("address.fxml"));

        try {
            Scene scene = new Scene(fxmlLoader.load(), 450, 400);
            Stage stage = new Stage();

            AddressController addressController = fxmlLoader.getController();
            addressController.setMainController(this);

            stage.setTitle("Change Address");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Apply settings immediately when they are saved
    public void applySettings() {
        String savedColor = AppStateManager.fetchProperty("backgroundColor");
        boolean dynamicTyping = Boolean.parseBoolean(AppStateManager.fetchProperty("dynamicTypingEnabled"));

        // Apply the background color change
        window.setStyle("-fx-background-color: " + savedColor);

        // Apply the dynamic typing visibility
        dynamicLabel.setVisible(dynamicTyping);
    }

    // Method to update background color
    public void updateBackgroundColor(Color color) {
        String hexColor = "#" + color.toString().substring(2);
        window.setStyle("-fx-background-color: " + hexColor);
    }

    // Method to update dynamic typing visibility
    public void updateDynamicTyping(boolean isEnabled) {
        dynamicLabel.setVisible(isEnabled);
    }


    // Start the server
    private void startServer(int port) {
        String currentIP = getLocalIPAddress();
        String IP = AppStateManager.fetchProperty("serverIP");

        assert currentIP != null;
        if (!currentIP.equals(IP)){
            NotificationSender.sendEmail(AppStateManager.fetchProperty(
                    "otherUserEmail"), currentIP);
            AppStateManager.updateProperty("serverIP", currentIP);
        }

        startTask(() -> {
            try (ServerSocket server = new ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"))) {
                // Notify the client before accepting the connection
                if (!hasSentNotification) {
                    String recipient = AppStateManager.fetchProperty("otherUserEmail");
                    NotificationSender.sendEmail(recipient);
                    hasSentNotification = true;
                }

                // Wait for the client to connect
                socket = server.accept();

                showChatScreen();

                // Initialize communication after client connection
                initializeCommunication();

            } catch (IOException e) {
                System.out.println("Server error: " + e.getMessage());
                try {
                    Thread.sleep(2000);
                    // Retry starting the server
                    startServer(port);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }



    // Start the client connection
    private void startClientConnection(String ip, int port) {
        startTask(() -> {
            long startTime = System.currentTimeMillis();
            boolean isConnected = false;

            while (!isConnected && (System.currentTimeMillis() - startTime <= WAIT_TIME_MS)) {
                try {
                    System.out.println("Attempting to connect to the server...");
                    socket = new Socket(InetAddress.getByName(ip), port);

                    // Connection established
                    System.out.println("Connected to the server!");
                    isConnected = true;

                    showChatScreen();

                    // Initialize communication
                    initializeCommunication();
                } catch (IOException e) {
                    System.out.println("Connection attempt failed. Retrying...");
                    try {
                        Thread.sleep(3000);

                        if (!hasSentNotification) {
                            String recipient = AppStateManager.fetchProperty("otherUserEmail");
                            NotificationSender.sendEmail(recipient);
                            hasSentNotification = true;
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (!isConnected) {
                System.out.println("Failed to connect within the timeout period.");
                Platform.runLater(this::showEndScreen);
            }
        });
    }


    // initializes the usernames and userIDs of all users in the .appdata file
    private void initializeCommunication() {
        try {
            input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // Start the thread responsible for reading messages
            startReadingThread();
        } catch (IOException e) {
            System.out.println("Communication setup error: " + e.getMessage());
        }
    }

    // Start reading thread
    public void startReadingThread() {
        startTask(() -> {
            try {
                String text;
                while (!socket.isClosed()) {
                    text = input.readLine();
                    if (text != null) {
                        if (text.startsWith("TYPING: ")) {
                            // Handle typing notifications
                            String finalText1 = text;
                            Platform.runLater(() -> {
                                dynamicLabel.setOpacity(1);
                                dynamicLabel.setText(finalText1);
                            });
                        } else if (text.equals("STOP")) {
                            // Handle stop typing notifications
                            Platform.runLater(() -> {
                                dynamicLabel.setOpacity(0);
                                dynamicLabel.setText("");
                            });
                        } else if (text.startsWith("EVENT: ")) {
                            // Handle event messages
                            String eventMessage = text.substring(7);  // Strip "EVENT: " prefix
                            String[] eventDetails = eventMessage.split(" ", 3); // Split into event name, description, and date

                            if (eventDetails.length == 3) {
                                String eventName = eventDetails[0];
                                String description = eventDetails[1];
                                ZonedDateTime eventDate = ZonedDateTime.parse(eventDetails[2]);

                                Platform.runLater(() -> {
                                    addMessageToContainer(
                                            "Event: " + eventName + "\n" +
                                                    "Description: " + description + "\n" +
                                                    "Date: " + eventDate,
                                            false, true);  // Display as event message
                                });
                            }
                        } else {
                            // Handle normal chat messages
                            String finalText = text;
                            Platform.runLater(() ->
                                    addMessageToContainer(finalText, false, false));

                            String otherUserID =
                                    AppStateManager.fetchProperty("otherUserID");

                            AppStateManager.saveMessage(otherUserID,
                                    AppStateManager.getUserID(), finalText);
                        }
                    } else {
                        showEndScreen();
                        closeResources();
                        System.exit(0);
                    }
                }
            } catch (IOException e) {
                System.err.println("Communication error: " + e.getMessage());
            } finally {
                Thread.currentThread().interrupt();
            }
        });
    }


    // Start sending messages in a separate thread
    public void startSendingThread() {
        startTask(() -> {
            try {
                String text = message.getText();
                if (text != null) {
                    output.println(text);
                }
            } catch (Exception e) {
                System.out.println("Error sending message: " + e.getMessage());
            }
        });
    }

    public void sendEventInvite(String eventName, String description,
                                ZonedDateTime zonedDateTime) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm a");
        String message = "EVENT: %s %s %s".formatted(eventName, description,
                dtf.format(zonedDateTime));
//        output.println(message);
        addMessageToContainer(message, true, false);
    }

    private void addMessageToContainer(String messageText, boolean isSent, boolean isEvent) {
        // Label for the message text
        Label messageLabel = new Label(messageText);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        messageLabel.setOpacity(0.85);

        // HBox to hold the message
        HBox hbox = new HBox();
        hbox.setSpacing(10);

        // If the message is an event, style it differently
        if (isEvent) {
            System.out.println("here");
            messageLabel.setStyle("-fx-background-color: #FFEB3B; " + // yellow background for events
                    "-fx-padding: 15px; " +
                    "-fx-border-radius: 15px;" +
                    "-fx-border-width: 2px;" +
                    "-fx-border-color: #FF9800;" +
                    "-fx-font-size: 16px;" +
                    "-fx-text-fill: #000000;" +
                    "-fx-font-family: Monaco, 'Courier New', monospace;");
        } else {
            messageLabel.setStyle("-fx-background-color: transparent; " +
                    "-fx-padding: 10px; " +
                    "-fx-border-radius: 15px;" +
                    "-fx-border-width: 2px;" +
                    "-fx-border-color: #000000;" +
                    "-fx-font-size: 16px;" +
                    "-fx-text-fill: #000000;" +
                    "-fx-font-family: Monaco, 'Courier New', monospace;");
        }

        // If the message was sent by the user, align it to the right, otherwise left
        if (isSent) {
            hbox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            hbox.setAlignment(Pos.CENTER_LEFT);
        }

        // Add message label to the HBox
        hbox.getChildren().add(messageLabel);

        // Add the message HBox to the container
        container.getChildren().add(hbox);

        // Bind the scroll pane to always show the latest message
        scrollPane.vvalueProperty().bind(container.heightProperty());
    }

    // Gracefully close the connection and sockets
    private void closeResources() {
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    // A method that handles threads by submitting it to the thread pool
    private void startTask(Runnable task) {
        executor.submit(task);
    }

    // A method to switch between waiting screen and chat screen
    private void showChatScreen() {
        Platform.runLater(() -> {
            waitingScreen.setVisible(false);  // Hide waiting screen
            notInstalledScreen.setVisible(false); // Hide not installed screen
            chatScreen.setVisible(true);      // Show chat screen
        });
    }

    // A method to switch to end screen and shut down the application gracefully
    private void showEndScreen() {
        chatScreen.setVisible(false);
        exitScreen.setVisible(true);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // A method to switch to waiting screen and show change address button
    // for client to change address in case IP of server changes
    private void showWaitingScreen() {
        addressScreen.setVisible(false); // Hide address screen
        waitingScreen.setVisible(true);

        if (AppStateManager.fetchProperty("admin").equals("false")){
            changeBtn.setVisible(true);
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
}
