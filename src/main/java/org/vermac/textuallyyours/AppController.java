package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import com.AppState.io.util.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
    private static final int WAIT_TIME_MS = 5 * 60 * 1000; // 5 minutes
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMMM, hh:mm a");
    private static final String style = "-fx-padding: 10px; " +
            "-fx-border-color: #000000; " +
            "-fx-font-size: 16px; " +
            "-fx-background-radius: 12px; " +
            "-fx-text-fill: #000000; " +
            "-fx-font-family: Monaco, 'Courier New', monospace; " +
            "-fx-opacity: 0.65; ";



    private BufferedReader input;
    private PrintWriter output;
    private Socket socket;
    private boolean hasSentNotification = false;

    String roomKey = AppStateManager.fetchProperty("roomKey");
    String initialised = AppStateManager.fetchProperty("initialised");
    String serverIP = AppStateManager.fetchProperty("serverIP");
    String admin = AppStateManager.fetchProperty("admin");
    String me = AppStateManager.fetchProperty("username");
    String otherUser = AppStateManager.fetchProperty("otherUser");
    String recipient = AppStateManager.fetchProperty("otherUserEmail");
    String savedColor = AppStateManager.fetchProperty("backgroundColor");
    String bubbleColor = AppStateManager.fetchProperty("bubbleColor");

    // FXML resources
    @FXML public StackPane window;
    @FXML private BorderPane chatScreen;
    @FXML private VBox waitingScreen;
    @FXML private VBox exitScreen;
    @FXML private VBox notInstalledScreen;
    @FXML private VBox addressScreen;
    @FXML private TextField message;
    @FXML private VBox container;
    @FXML private ScrollPane scrollPane;
    @FXML private Label dynamicLabel;

    // Start the application
    public void initialize() {
        if (initialised.equals("true")) {
            AppStateManager.initializeApp();
            applySettings();

            showWaitingScreen();

            int port = Integer.parseInt(roomKey);
            String IP = serverIP;
            boolean isAdmin = Boolean.parseBoolean(admin);

            List<Message> messageList = AppStateManager.loadMessages();

            // Load messages and put them on the screen
            for (Message m : messageList) {
                String sender = m.getSender();
                String content = m.getContent();
                addMessageToContainer(content,
                        sender.equals(AppStateManager.getUserID()), false);
            }

            if (isAdmin) {
                startServer(port, 5000);
            } else {
                addressScreen.setVisible(true);
                startClientConnection(IP, port);
            }

            dynamicLabel.setVisible(false);

            // Dynamic typing
            message.textProperty().addListener((observableValue, oldValue, newValue) -> {
                if (!newValue.trim().isEmpty()) {
                    output.println("T: " + newValue);
                } else {
                    output.println("STOP");
                }
            });

        } else {
            notInstalledScreen.setVisible(true);
        }
    }

    // Handle the text message by adding it to the screen
    public void handleText() {
        if (message.getText() != null && !message.getText().trim().isEmpty()) {
            startSendingThread();
            AppStateManager.saveMessage(AppStateManager.getUserID(), AppStateManager.fetchProperty("otherUserID"), message.getText().trim());
            addMessageToContainer(message.getText(), true, false);
            message.clear();
        }
    }

    // Settings and Event Handlers
    public void handleSettings() { openWindow("settings.fxml", "Settings", 550, 350); }
    public void handleEvent() { openWindow("event.fxml", "Plan An Event", 550, 350); }
    public void handleAddress() { openWindow("address.fxml", "Change Address", 450, 400); }

    // Apply settings immediately when they are saved
    public void applySettings() {
        savedColor = AppStateManager.fetchProperty("backgroundColor");
        bubbleColor = AppStateManager.fetchProperty("bubbleColor");

        window.setStyle("-fx-background-color: " + savedColor);
        changeBubbleColor(bubbleColor);
    }

    // Method to update settings
    public void updateBackgroundColor(Color color) {
        String hexColor = "#" + color.toString().substring(2);
        window.setStyle("-fx-background-color: " + hexColor);
    }

    public void updateBubbleColor(Color color) {
        String hexColor = "#" + color.toString().substring(2);
        changeBubbleColor(hexColor);
    }

    // Start the server
    private void startServer(int port, int timeout) {
        String currentIP = getLocalIPAddress();
        assert currentIP != null;

        if (!currentIP.equals(serverIP)) {
            NotificationSender.sendEmail(recipient, "address", currentIP);
            AppStateManager.updateProperty("serverIP", currentIP);
        }

        startTask(() -> {
            try (ServerSocket server = new ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"))) {
                server.setSoTimeout(timeout);
                socket = server.accept();
                showChatScreen();
                initializeCommunication();
            } catch (IOException e) {
                System.out.println("Server error: " + e.getMessage());

                if (!hasSentNotification) {
                    NotificationSender.sendEmail(recipient);
                    hasSentNotification = true;
                }
                startServer(port, WAIT_TIME_MS);
            }
        });
    }

    // Start the client connection
    private void startClientConnection(String ip, int port) {

        startTask(() -> {
            long startTime = System.currentTimeMillis();
            boolean hasSentNotification = false;

            while (true) {
                try {
                    // Try connecting to the server
                    socket = new Socket(InetAddress.getByName(ip), port);

                    showChatScreen();
                    initializeCommunication();
                    break;
                } catch (IOException e) {
                    long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;

                    // Send notification only once after 5 seconds
                    if (elapsedTime >= 5 && !hasSentNotification) {
                        NotificationSender.sendEmail(recipient);
                        hasSentNotification = true;
                    }

                    // Wait for 1 second before retrying
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
    }

    // Initializes communication between the client and server
    private void initializeCommunication() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            startReadingThread();
        } catch (IOException e) {
            System.out.println("Communication setup error: " + e.getMessage());
        }
    }

    // Start reading messages in a separate thread
    public void startReadingThread() {
        startTask(() -> {
            try {
                String text;
                while (!socket.isClosed()) {
                    text = input.readLine();
                    if (text != null) {
                        processIncomingMessage(text);
                    } else {
                        closeResources();
                        showEndScreen();
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
                    output.println("N: " + text);
                }
            } catch (Exception e) {
                System.out.println("Error sending message: " + e.getMessage());
            }
        });
    }

    // Event and Event Confirmation Handlers
    public void sendEventInvite(String eventName, ZonedDateTime zonedDateTime) {
        String message = "EVENT: " + "\nYou are planning an event!!" +
                "\nname: " + eventName + "\ntime: " + dtf.format(zonedDateTime);
        String[] outputArray = {eventName, zonedDateTime.toString()};
        String outputMessage = Arrays.toString(outputArray);

        output.println("E: " + outputMessage);
        addMessageToContainer(message.substring(8), true, true);
    }

    private void event(String text) {
        String eventMessage = text.substring(3);
        String[] eventDetails = eventMessage.substring(1, eventMessage.length() - 1).split(", ");
        String eventName = eventDetails[0];
        String time = dtf.format(ZonedDateTime.parse(eventDetails[1]));

        Platform.runLater(() -> displayEventAlert(eventName, time));
    }

    private void eventConfirmation(String text) {
        text = text.substring(5);

        Label event = (Label) container.lookup("#eventLabel");
        if (text.startsWith("YES.")) {
            String[] eventDetails =
                    text.substring(5, text.length() - 1).split(", ", 4);
            Platform.runLater(() -> event.setText("Aww, it's confirmed! 🎉 You're in for a fun time! 🥳"));
            NotificationSender.sendEmail(recipient, "event", eventDetails[1], eventDetails[0], eventDetails[2], eventDetails[3]);
        } else {
            Platform.runLater(() -> {
                event.setText("Aww, maybe next time! 😢");
                event.setId("");
            });
        }
    }

    private void processIncomingMessage(String text) {
        if (text.startsWith("T: ")) {
            typing(text);
        } else if (text.startsWith("STOP")) {
            stop();
        } else if (text.startsWith("E: ")) {
            event(text);
        } else if (text.startsWith("E_C: ")) {
            eventConfirmation(text);
        } else if (text.startsWith("N: ")) {
            Platform.runLater(() -> addMessageToContainer(text.substring(3),
                    false, false));
            String otherUserID = AppStateManager.fetchProperty("otherUserID");
            AppStateManager.saveMessage(otherUserID, AppStateManager.getUserID(), text.substring(3));
        }
    }

    // Method to add messages to the container
    private void addMessageToContainer(String messageText, boolean isSent, boolean isEvent) {
        Label messageLabel = new Label(messageText.trim());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(450);

        HBox hbox = new HBox();
        hbox.setSpacing(10);
        if (isEvent) {
            messageLabel.setOpacity(0.85);
            messageLabel.setStyle(style + "-fx-background-color: " + bubbleColor + "; " +
                    "-fx-border-radius: 12px; " +
                    "-fx-border-width: 2px; " +
                    "-fx-opacity: 0.7; ");

            if (isSent) {
                messageLabel.setId("eventLabel");
                messageLabel.setText(messageLabel.getText() + "\n\nWaiting for " + otherUser + "'s response...");
            }
        } else {
            messageLabel.setStyle(style + "-fx-background-color: " + bubbleColor + "; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 2px; ");
        }

        if (isSent) {
            hbox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            hbox.setAlignment(Pos.CENTER_LEFT);
        }

        hbox.getChildren().add(messageLabel);
        container.getChildren().add(hbox);
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

    // Utility methods
    private void startTask(Runnable task) {
        executor.submit(task);
    }

    private static String getLocalIPAddress() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Something went wrong: " + e.getMessage());
        }
        return null;
    }

    private void openWindow(String fxml, String title, int width, int height) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource(fxml));

        try {
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            Stage stage = new Stage();
            stage.setResizable(false);

            switch (fxml) {
                case "address.fxml" -> {
                    AddressController addressController = fxmlLoader.getController();
                    addressController.setMainController(this);
                }
                case "settings.fxml" -> {
                    SettingsController settingsController = fxmlLoader.getController();
                    settingsController.setMainController(this);
                    stage.setOnCloseRequest(event -> applySettings());
                }
                case "event.fxml" -> {
                    EventController eventController = fxmlLoader.getController();
                    eventController.setMainController(this);
                }
            }

            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void showWaitingScreen() {
        waitingScreen.setVisible(true);
        if (admin.equals("false")){
            System.out.println("Test");
            addressScreen.setVisible(true);
        }
    }

    private void showChatScreen() {
        Platform.runLater(() -> {
            waitingScreen.setVisible(false);
            addressScreen.setVisible(false);
            chatScreen.setVisible(true);
        });
    }

    private void showEndScreen() {
        chatScreen.setVisible(false);
        exitScreen.setVisible(true);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void typing(String text) {
        Platform.runLater(() -> {
            dynamicLabel.setText("TYPING: " + text.substring(3));
            dynamicLabel.setVisible(true);
        });
    }

    private void stop() {
        Platform.runLater(() -> {
            dynamicLabel.setVisible(false);
            dynamicLabel.setText("");
        });
    }

    private void displayEventAlert(String eventName, String time) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("A Special Invitation!");
        alert.setHeaderText(otherUser + " is planning something special! Are you in?");
        alert.setContentText("Event: %s\nTime: %s".formatted(eventName, time));

        ButtonType yesButton = new ButtonType("I'd Love To!");
        ButtonType noButton = new ButtonType("Some other time :(");

        String buttonStyle = "-fx-border-width: 1px; -fx-border-color: " +
                "black; -fx-border-radius: 5px; -fx-background-color: " +
                "transparent; -fx-font-family: Monaco, 'Courier New', " +
                "monospace; -fx-font-size: 14px";
        String labelStyle = "-fx-font-family: Monaco, 'Courier New', " +
                "monospace; -fx-font-size: 16px;";
        String contentStyle = "-fx-font-family: Monaco, 'Courier New', " +
                "monospace; -fx-font-size: 16px; -fx-padding: 0px 0px 10px 10px";

        alert.getButtonTypes().setAll(yesButton, noButton);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setGraphic(null);
        dialogPane.setStyle("-fx-background-color: " + savedColor);
        dialogPane.lookupButton(yesButton).setStyle(buttonStyle);
        dialogPane.lookupButton(noButton).setStyle(buttonStyle);

        Region headerPanel = (Region) dialogPane.lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle("-fx-background-color: " + savedColor);
        }

        Label header = (Label) dialogPane.lookup(".header-panel .label");
        if (header != null) {
            header.setStyle(labelStyle + "-fx-font-size: 16px");
        }

        Label content = (Label) dialogPane.lookup(".content.label");
        if (content != null) {
            content.setStyle(contentStyle);
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == yesButton) {
                String[] details = {otherUser, me, eventName, time};
                output.println("E_C: YES." + Arrays.toString(details));

                startTask(() -> NotificationSender.sendEmail(recipient, "event", otherUser, me, eventName, time));
            } else {
                output.println("E_C: NO.");
            }
        });
    }

    private void changeBubbleColor(String color) {
        var messages = container.getChildren();

        for (var node : messages) {
            HBox messageBox = (HBox) node;
            for (var message : messageBox.getChildren()) {
                message.setStyle(style + "-fx-background-color: "+color+";" +
                        "-fx-border-radius: 12px; " +
                        "-fx-border-width: 2px;");
            }
        }
    }
}