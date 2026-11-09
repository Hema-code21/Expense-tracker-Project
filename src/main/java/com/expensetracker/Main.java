package com.expensetracker;

import java.util.Scanner;

/**
 * Application entry point.
 *
 * <p>Creates a {@link Scanner} bound to stdin, hands it to {@link MenuHandler},
 * and starts the interactive loop.  Any uncaught exception is printed with a
 * friendly message so the stack trace is visible during development but the
 * program exits gracefully.</p>
 *
 * <p><strong>Running the application:</strong>
 * <pre>
 *   # compile and package
 *   mvn package -DskipTests
 *
 *   # run the fat JAR
 *   java -jar target/expense-tracker-all.jar
 *
 *   # or run directly with Maven
 *   mvn exec:java -Dexec.mainClass="com.expensetracker.Main"
 * </pre>
 * </p>
 */
public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            MenuHandler menuHandler = new MenuHandler(scanner);
            menuHandler.run();
        } catch (Exception e) {
            System.err.println("\n[FATAL] An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
