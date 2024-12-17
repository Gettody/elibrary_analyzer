package elibraryparser;
import java.sql.*;

public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:authors.db";
    private static final String TABLE_NAME = "authors";

    public DatabaseManager() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT," +
                "publishesCount INTEGER," +
                "zeroCittPublishesCount INTEGER," +
                "hirshIndex INTEGER" +
                ");";
        executeStatement(createTableSQL);
    }

    private void executeStatement(String sql) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("SQL ошибка при запросе: " + e.getMessage());
        }
    }

    private boolean executeQueryForExists(String sql, int id) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("SQL ошибка при запросе: " + e.getMessage());
        }
        return false;
    }



    public boolean recordExists(int id) {
        String checkRecordSQL = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";
        return executeQueryForExists(checkRecordSQL, id);
    }

    public boolean addAuthor(Author author) {
        int id = author.authorId();
        String name = author.name();
        int publishes = author.publishesCount();
        int zeroCittPublishesCount = author.zeroCittPublishesCount();
        int hIndex = author.hirshIndex();

        if (addRecord(id, name, publishes, zeroCittPublishesCount, hIndex)) {
            return  true;
        } else {
            return false;
        }
    }

    public boolean addRecord(int id, String name, int publishes, int zeroCittPublishesCount, int hIndex) {
        if (recordExists(id)) {
            System.out.println("Запись с id " + id + " уже существует.");
            return false;
        }
        String insertRecordSQL = "INSERT INTO " + TABLE_NAME + " (id, name, publishesCount, zeroCittPublishesCount, hirshIndex) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(insertRecordSQL)) {
            preparedStatement.setInt(1, id);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, publishes);
            preparedStatement.setInt(4, zeroCittPublishesCount);
            preparedStatement.setInt(5, hIndex);
            preparedStatement.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL ошибка при добавлении автора: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRecord(int id) {
        if (!recordExists(id)) {
            System.out.println("Записи с id " + id + " не существует.");
            return false;
        }

        String deleteRecordSQL = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(deleteRecordSQL)) {
            preparedStatement.setInt(1, id);
            int affectedRows = preparedStatement.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка при удалении автора: " + e.getMessage());
            return false;
        }
    }
    public Author getAuthor(int id) {
        String selectAuthorSQL = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(selectAuthorSQL)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                String name = resultSet.getString("name");
                int publishes = resultSet.getInt("publishesCount");
                int zeroCitiPublishes = resultSet.getInt("zeroCittPublishesCount");
                int hIndex = resultSet.getInt("hirshIndex");
                return new Author(id, name, publishes, zeroCitiPublishes, hIndex);
            }
        } catch (SQLException e) {
            System.err.println("SQL ошибка при запросе автора: " + e.getMessage());
        }
        return null;
    }
}
