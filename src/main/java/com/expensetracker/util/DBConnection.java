package com.expensetracker.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages JDBC connections to the MySQL database.
 *
 * <p>Configuration is loaded from {@code db.properties} on the classpath
 * (or from the project root directory). This file must never be committed
 * to source control. Alternatively, set the following environment variables:
 * <ul>
 *   <li>{@code DB_URL}      – full JDBC URL</li>
 *   <li>{@code DB_USERNAME} – database user</li>
 *   <li>{@code DB_PASSWORD} – database password</li>
 * </ul>
 * Environment variables take precedence over the properties file.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * try (Connection conn = DBConnection.getConnection()) {
 *     // use connection...
 * }
 * }</pre>
 * </p>
 */
public final class DBConnection {

    // Property file expected at project root or on the classpath
    private static final String PROPS_FILE = "db.properties";

    private static String url;
    private static String username;
    private static String password;

    static {
        loadConfig();
    }

    // Prevent instantiation
    private DBConnection() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a new JDBC {@link Connection}.
     *
     * <p>The caller is responsible for closing the connection (best done via
     * try-with-resources).</p>
     *
     * @return a valid {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Loads database credentials. Environment variables override the file.
     */
    private static void loadConfig() {
        // 1) Try environment variables first
        String envUrl  = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USERNAME");
        String envPass = System.getenv("DB_PASSWORD");

        if (envUrl != null && envUser != null && envPass != null) {
            url      = envUrl;
            username = envUser;
            password = envPass;
            return;
        }

        // 2) Fall back to db.properties on the classpath
        Properties props = new Properties();

        // Try loading from classpath
        try (InputStream is = DBConnection.class.getClassLoader()
                .getResourceAsStream(PROPS_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {
            // Will attempt filesystem load below
        }

        // Try loading from project root (convenient during development)
        if (props.isEmpty()) {
            try (InputStream is = DBConnection.class.getResourceAsStream("/" + PROPS_FILE)) {
                if (is != null) props.load(is);
            } catch (IOException ignored) {}
        }

        // Also try from current working directory
        if (props.isEmpty()) {
            java.io.File file = new java.io.File(PROPS_FILE);
            if (file.exists()) {
                try (InputStream is = new java.io.FileInputStream(file)) {
                    props.load(is);
                } catch (IOException ignored) {}
            }
        }

        url      = props.getProperty("DB_URL",      "jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        username = props.getProperty("DB_USERNAME", "root");
        password = props.getProperty("DB_PASSWORD", "");

        // Override with any env variables that ARE present
        if (envUrl  != null) url      = envUrl;
        if (envUser != null) username = envUser;
        if (envPass != null) password = envPass;
    }
}
