package ApiTests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.revature.repository.DatabaseConnection;

public class DummyDataLoader {

    private final DatabaseConnection databaseConnection;

    public DummyDataLoader() {
        databaseConnection = new DatabaseConnection();
    }

    public void restoreDatabase() throws SQLException {

//        if (!databaseConnection.isTestMode()) {
//            return;
//        }

        try {
            // Close any existing connections before copying
            Connection connection = databaseConnection.getConnection();

            Path sqlFilePath = Paths.get("src/test/resources/test_data.sql"); // Adjust path as needed
            String sqlContent = new String(Files.readAllBytes(sqlFilePath), StandardCharsets.UTF_8);

            // Split SQL file by semicolons and execute each statement
            String[] statements = sqlContent.split(";");
            try (Statement stmt = connection.createStatement()) {
                for (String sql : statements) {
                    String trimmedSql = sql.trim();
                    if (!trimmedSql.isEmpty() && !trimmedSql.startsWith("--")) {
                        stmt.execute(trimmedSql);
                    }
                }
            }

        } catch (IOException e) {
            throw new SQLException("Failed to copy database file: " + e.getMessage(), e);
        }
    }
}
