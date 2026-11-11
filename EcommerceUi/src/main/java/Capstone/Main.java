package Capstone;

import Capstone.ui.LoginDialog;
import Capstone.ui.MainFrame;
import Capstone.ui.Theme;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            start();
        });
    }

    public static void start() {
        LoginDialog login = new LoginDialog();
        login.setVisible(true);

        if (!login.isSignedIn()) {
            System.exit(0);
        }

        new MainFrame().setVisible(true);
    }
}
