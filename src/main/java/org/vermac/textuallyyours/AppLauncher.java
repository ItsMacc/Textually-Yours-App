package org.vermac.textuallyyours;

import com.AppState.io.AppStateManager;
import javafx.application.Application;
import org.vermac.textuallyyoursinstaller.InstallerApplication;

public class AppLauncher {
    public static void main(String[] args) {

        if (AppStateManager.canStartApp()) {
            Application.launch(TextuallyYoursApplication.class);
        } else {
            Application.launch(InstallerApplication.class);
        }
    }
}
