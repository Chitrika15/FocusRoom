package com.focusroom.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connectivity using JDBC.
 * Reads connection settings from db.properties or environment overrides.
 */
public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    static {
        loadConfiguration();
    }

    /**
     * Loads database configuration from classpath or local file.
     */
    public static synchronized void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try classpath resource
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load db.properties from classpath: " + e.getMessage());
        }

        // 2. If empty, try current working directory db.properties
        if (props.isEmpty()) {
            File localFile = new File("db.properties");
            if (localFile.exists()) {
                try (FileInputStream fis = new FileInputStream(localFile)) {
                    props.load(fis);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Could not load local db.properties: " + e.getMessage());
                }
            }
        }

        // Environment variable / System property overrides or fallbacks
        dbUrl = System.getProperty("db.url", props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/focusroom?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"));
        dbUser = System.getProperty("db.user", props.getProperty("db.user", "root"));
        dbPassword = System.getProperty("db.password", props.getProperty("db.password", "root"));

        // Register driver
        try {
            if (dbUrl.contains(":mysql:")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else if (dbUrl.contains(":h2:")) {
                Class.forName("org.h2.Driver");
            }
            initialized = true;
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "JDBC Driver class not found: " + e.getMessage(), e);
        }
    }

    /**
     * Allows custom configuration override (e.g. for unit testing).
     */
    public static synchronized void setCustomConnection(String url, String user, String password) {
        dbUrl = url;
        dbUser = user;
        dbPassword = password;
        initialized = true;
    }

    /**
     * Obtains a new database connection.
     *
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Validates if the database is reachable.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database connection check failed: " + e.getMessage());
            return false;
        }
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }
}
