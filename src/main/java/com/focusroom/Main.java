package com.focusroom;

import com.focusroom.ui.LoginFrame;
import com.focusroom.util.DBConnection;
import com.focusroom.web.WebServer;

import javax.swing.*;
import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Main application entry point for FocusRoom - Virtual Study Rooms.
 * Starts the embedded Web Server at http://localhost:8080 and provides Swing GUI.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final int WEB_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("                 🎯 FocusRoom - Virtual Study Rooms                   ");
        System.out.println("======================================================================");

        // 1. Test database connectivity
        boolean dbConnected = DBConnection.testConnection();
        if (!dbConnected) {
            System.err.println("  WARNING: Could not connect to MySQL at: " + DBConnection.getDbUrl());
            System.err.println("  Please ensure MySQL is running and verify 'db.properties'.");
        } else {
            System.out.println("  [MySQL] Connected successfully: " + DBConnection.getDbUrl());
        }

        // 2. Start Embedded Web Server on port 8080
        try {
            WebServer webServer = new WebServer(WEB_PORT);
            webServer.start();

            System.out.println("----------------------------------------------------------------------");
            System.out.println("  🌐 WEB APPLICATION IS LIVE AT:");
            System.out.println("  👉 http://localhost:" + WEB_PORT);
            System.out.println("----------------------------------------------------------------------");

            // Automatically open default browser to localhost:8080
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + WEB_PORT));
                }
            } catch (Exception e) {
                LOGGER.info("Could not auto-open browser: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("  Failed to start web server on port " + WEB_PORT + ": " + e.getMessage());
        }

        // 3. Launch Swing GUI Desktop App
        boolean startGui = true;
        for (String arg : args) {
            if ("--no-gui".equalsIgnoreCase(arg)) {
                startGui = false;
                break;
            }
        }

        if (startGui) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            SwingUtilities.invokeLater(() -> {
                try {
                    LoginFrame loginFrame = new LoginFrame();
                    loginFrame.setVisible(true);
                } catch (Exception ex) {
                    LOGGER.info("Swing GUI notice: " + ex.getMessage());
                }
            });
        }
    }
}
