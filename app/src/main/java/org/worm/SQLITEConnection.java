package org.worm;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
public class SQLITEConnection {
    private static Connection connection;
    private static final Object lock = new Object(); // To ensure thread safety
    private SQLITEConnection(String DB_URL)
    {

    }

    public static Connection getConnection(String DB_URL) throws SQLException {
        if (connection == null || connection.isClosed()) {
            synchronized (lock) { // Ensuring thread safety
                if (connection == null || connection.isClosed()) {
                    connection = DriverManager.getConnection(DB_URL);
                }
            }
        }
        return connection;
    }

}
