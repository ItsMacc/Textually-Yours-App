package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import com.AppState.io.util.Message;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppController {

    // Instance variables
    private static final int WAIT_TIME_MS = 5 * 60 * 1000; // 5 seconds
    private static final ExecutorService executor =
            Executors.newCachedThreadPool();
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMMM, hh:mm a");


    private BufferedReader input;
    private PrintWriter output;
    private Socket socket;
    private boolean hasSentNotification = true;

    String roomKey = AppStateManager.fetchProperty("roomKey");
    String initialised = AppStateManager.fetchProperty("initialised");
    String serverIP = AppStateManager.fetchProperty("serverIP");
    String admin = AppStateManager.fetchProperty("admin");
    String dynamicTypingEnabled = AppStateManager.fetchProperty("dynamicTypingEnabled");
    String me = AppStateManager.fetchProperty("username");
    String otherUser = AppStateManager.fetchProperty("otherUser");
    String recipient = AppStateManager.fetchProperty("otherUserEmail");
    String savedColor = AppStateManager.fetchProperty("backgroundColor");

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
//        AppStateManager.initializeApp();
//        applySettings();
//
//        showWaitingScreen();
//        showChatScreen();
        if (initialised.equals("true")) {

            // Initialize the app and apply saved/default settings
            AppStateManager.initializeApp();
            applySettings();

            showWaitingScreen();

            int port = Integer.parseInt(roomKey);
            String IP = serverIP;
            boolean isAdmin = Boolean.parseBoolean(admin);

            List<Message> messageList = AppStateManager.loadMessages();

            for (Message m : messageList) {
                String sender = m.getSender();
                String content = m.getContent();
                addMessageToContainer(content, sender.equals(AppStateManager.getUserID()), false);
            }

            if (isAdmin) {
                startServer(port);
            } else {
                startClientConnection(IP, port);
            }

            // Listen for messages to display dynamically
            message.textProperty().addListener((observableValue,
                                                oldValue,
                                                newValue) -> {
                if (!newValue.trim().isEmpty()) {
                    output.println("TYPING: " + newValue);
                } else {
                    output.println("STOP");
                }
            });
        }
        else {
            window.setStyle("-fx-background-color: #a459cd");
            notInstalledScreen.setVisible(true);
        }
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
        openWindow("settings.fxml", "Settings", 550, 350);
    }

    public void handleEvent() {
        openWindow("event.fxml", "Plan An Event", 550, 350);
    }

    public void handleAddress(){
        openWindow("address.fxml", "Change Address", 450, 400);
    }

    // Apply settings immediately when they are saved
    public void applySettings() {
        // Apply the background color change
        window.setStyle("-fx-background-color: " + savedColor);

        // Apply the dynamic typing visibility
        dynamicLabel.setVisible(dynamicTypingEnabled.equals("true"));
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

        assert currentIP != null;
        if (!currentIP.equals(serverIP)){
            NotificationSender.sendEmail(recipient, currentIP);
            AppStateManager.updateProperty("serverIP", currentIP);
        }

        startTask(() -> {
            try (ServerSocket server = new ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"))) {
                // Notify the client before accepting the connection
                if (!hasSentNotification) {
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
                        processIncomingMessage(text);
                    }
                    else {
                        closeResources();
                        showEndScreen();
                        Platform.runLater(() -> System.exit(0));
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
                    output.println("NORMAL: " + text);
                }
            } catch (Exception e) {
                System.out.println("Error sending message: " + e.getMessage());
            }
        });
    }

    public void sendEventInvite(String eventName, ZonedDateTime zonedDateTime) {
        String message = "EVENT: " + "\nYou are planning an event!!" +
                "\nname: " + eventName + "\ntime: " + dtf.format(zonedDateTime);

        String[] outputArray = {eventName, zonedDateTime.toString()};
        String outputMessage = Arrays.toString(outputArray);

        output.println("EVENT: " + outputMessage);
        System.out.println(outputMessage);

        addMessageToContainer(message.substring(8), true, true);
    }

    private void addMessageToContainer(String messageText, boolean isSent, boolean isEvent) {
        // Label for the message text
        Label messageLabel = new Label(messageText);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(450);
        messageLabel.setOpacity(0.85);

        // HBox to hold the message
        HBox hbox = new HBox();
        hbox.setSpacing(10);

        if (isEvent) {
            messageLabel.setStyle("-fx-background-color: #eded98; " +
                    "-fx-padding: 15px; " +
                    "-fx-border-radius: 15px;" +
                    "-fx-background-radius: 15px;" +
                    "-fx-border-width: 2px;" +
                    "-fx-border-color: #FF9800;" +
                    "-fx-font-size: 16px;" +
                    "-fx-text-fill: #000000;" +
                    "-fx-font-family: Monaco, 'Courier New', monospace;");
            if (isSent) {
                messageLabel.setId("eventLabel");
                messageLabel.setText(messageLabel.getText() + "\n\nWaiting " +
                        "for " + otherUser + "'s response...");
            }
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

        if (admin.equals("false")){
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

    private void openWindow(String fxml, String title, int width, int height){
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource(fxml));

        try {
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            Stage stage = new Stage();

            if (fxml.equals("address.fxml")) {
                AddressController addressController = fxmlLoader.getController();
                addressController.setMainController(this);
            }
            if (fxml.equals("settings.fxml")) {
                SettingsController settingsController = fxmlLoader.getController();
                settingsController.setMainController(this);
            }
            if (fxml.equals("event.fxml")) {
                EventController eventController = fxmlLoader.getController();
                eventController.setMainController(this);
            }

            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            stage.setOnCloseRequest(windowEvent -> applySettings());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void typing(String text) {
        Platform.runLater(() -> {
            dynamicLabel.setOpacity(1);
            dynamicLabel.setText(text);
        });
    }

    private void stop() {
        Platform.runLater(() -> {
            dynamicLabel.setOpacity(0);
            dynamicLabel.setText("");
        });
    }

    private void event(String text) {
        String eventMessage = text.substring(7);

        // Parse array string back to an array
        String[] eventDetails =
                eventMessage.substring(1, eventMessage.length()-1).split(", ");

        String eventName = eventDetails[0];
        String time = dtf.format(ZonedDateTime.parse(eventDetails[1]));

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

            alert.setTitle("A Sweet Request");
            alert.setHeaderText(otherUser + " " +
                    "is planning something special. Are you in?");
            alert.setContentText("""
                    Details:
                    Event: %s
                    Time: %s
                    """.formatted(eventName, time));

            ButtonType yesButton = new ButtonType("I'd Love To!");
            ButtonType noButton = new ButtonType("Some other time");
            alert.getButtonTypes().setAll(yesButton, noButton);

            // Handle response
            alert.showAndWait().ifPresent(response -> {
                if (response == yesButton){
                    String[] details = {otherUser, me, eventName, time};

                    output.println("EVENT_CONF: YES." + Arrays.toString(details));

                    NotificationSender.sendEmail(recipient, "event",
                            me, otherUser, eventName, time);
                } else {
                    output.println("EVENT_CONF: NO.");
                }
            });

        });
    }

    private void eventConfirmation(String text) {
        Label event = (Label) container.lookup("#eventLabel");

        if (text.startsWith("YES.")) {
            String[] eventDetails = text.substring(4, text.length() - 1).split(", ");

            Platform.runLater(() -> {
                event.setText("Aww, it's confirmed! 🎉 You're in for a fun time! 🥳");
            });

            startTask(() -> NotificationSender.sendEmail(recipient, "event",
                    eventDetails[0],
                    eventDetails[1],
                    eventDetails[2],
                    eventDetails[3]));

        } else {
            Platform.runLater(() -> event.setText("Aww, maybe next time! 😢"));
        }
    }

    private void processIncomingMessage(String text){
        if (text.startsWith("TYPING: ")) {
            typing(text);
        } else if (text.startsWith("STOP")) {
            stop();
        } else if (text.startsWith("EVENT: ")) {
            event(text);
        } else if (text.startsWith("EVENT_CONF: ")) {
            eventConfirmation(text.substring(12));
            System.out.println("done");
        } else if (text.startsWith("NORMAL: ")) {
            System.out.println("normal");
            Platform.runLater(() -> addMessageToContainer(text.substring(8),
                    false, false));

            String otherUserID = AppStateManager.fetchProperty("otherUserID");
            AppStateManager.saveMessage(otherUserID,
                    AppStateManager.getUserID(),
                    text.substring(8));
        }
    }
}