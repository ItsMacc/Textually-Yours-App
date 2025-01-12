module org.vermac.textuallyyours {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires AppState21;
    requires java.sql;
    requires javax.mail;
    requires java.activation;

    opens org.vermac.textuallyyours to javafx.fxml;
    exports org.vermac.textuallyyours;

    opens org.vermac.textuallyyoursinstaller to javafx.fxml;
    exports org.vermac.textuallyyoursinstaller;
}