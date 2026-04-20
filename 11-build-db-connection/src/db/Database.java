package db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class Database {
    private static final String URL = "jdbc:sqlite:sample.db";

    public static Connection getConnection() throws SQLException {
        Driver driver = new org.sqlite.JDBC();
        Connection connection = driver.connect(URL, new Properties());

        if (connection == null) {
            throw new SQLException("Failed to create SQLite connection for URL: " + URL);
        }
        return connection;
    }
}
