package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class AddressController {
    private AppController mainAppController;

    @FXML
    private StackPane window;
    @FXML
    private TextField address;
    @FXML
    private Label displayText;


    public void initialize(){
        String savedColor = AppStateManager.fetchProperty("backgroundColor");
        window.setStyle("-fx-background-color: " + savedColor);

        address.setOnKeyPressed(keyEvent -> {
            displayText.setVisible(false);
            displayText.setText("");
        });
    }

    public void setMainController(AppController mainAppController){
        this.mainAppController = mainAppController;
    }

    public void changeAddress(ActionEvent event) {
        String newIP = address.getText().trim();
        address.clear();

        if (!newIP.isEmpty()) {
            int port = Integer.parseInt(AppStateManager.fetchProperty("roomKey"));

            displayText.setText("Trying to connect...");
            displayText.setVisible(true);

            try {

                Socket socket = new Socket(InetAddress.getByName(newIP),port);

                String otherUserID = AppStateManager.fetchProperty(
                        "otherUserID");

                if (otherUserID.equals(AppStateManager.getUserID())) {
                    displayText.setVisible(true);
                    displayText.setText("Connected to user! Please close the app " +
                            "and open it again!");
                    AppStateManager.updateProperty("serverIP", newIP);
                } else {
                    displayText.setText("Nope! This is not YOUR partner. " +
                            "Trying to be sneaky, huh? Go find your ACTUAL " +
                            "one and only");
                }

                socket.close();
                }
            catch (IOException e) {
                displayText.setText("Cannot connect to the new address. " +
                        "Please check the address again");
            }
        }
    }
}
