package elibraryparser;

import lombok.extern.log4j.Log4j2;

import java.sql.*;

@Log4j2
public class DatabaseManager {

    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:authors.db";
    private final String databaseUrl;
    private static final String TABLE_NAME = "authors";

    public DatabaseManager() {
        this(DEFAULT_DATABASE_URL);
    }

    protected DatabaseManager(String databaseUrl) {
        this.databaseUrl = databaseUrl;
        log.info("Инициализация DatabaseManager с URL: {}", databaseUrl);
        createTableIfNotExists();
        log.info("DatabaseManager инициализирован");
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
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            log.debug("Выполнение SQL запроса: {}", sql);
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.error("SQL ошибка при запросе: ", e);
        }
    }

    private boolean executeQueryForExists(String sql, int id) {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            log.debug("Выполнение SQL запроса для проверки существования записи: {} с id: {}", sql, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("SQL ошибка при запросе: ", e);
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
            log.info("Автор {} добавлен в базу данных", id);
            return true;
        } else {
            log.info("Автор {} не был добавлен в базу данных", id);
            return false;
        }
    }

    public boolean addRecord(int id, String name, int publishes, int zeroCittPublishesCount, int hIndex) {
        if (recordExists(id)) {
            log.info("Запись с authorId:{} уже существует", id);
            return false;
        }
        String insertRecordSQL = "INSERT INTO " + TABLE_NAME + " (id, name, publishesCount, zeroCittPublishesCount, hirshIndex) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = connection.prepareStatement(insertRecordSQL)) {
            preparedStatement.setInt(1, id);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, publishes);
            preparedStatement.setInt(4, zeroCittPublishesCount);
            preparedStatement.setInt(5, hIndex);
            log.debug("Выполнение SQL запроса на добавление записи: {} с параметрами: id={}, name={}, publishes={}, zeroCitt={}, hIndex={}", insertRecordSQL, id, name, publishes, zeroCittPublishesCount, hIndex);
            preparedStatement.executeUpdate();
            log.info("Запись {} добавлена", id);
            return true;
        } catch (SQLException e) {
            log.error("SQL ошибка при добавлении записи: ", e);
            return false;
        }
    }

    public boolean deleteRecord(int id) {
        if (!recordExists(id)) {
            log.info("Запись {} не удалена, запись не существует", id);
            return false;
        }

        String deleteRecordSQL = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = connection.prepareStatement(deleteRecordSQL)) {
            preparedStatement.setInt(1, id);
            log.debug("Выполнение SQL запроса на удаление записи: {} с id: {}", deleteRecordSQL, id);
            int affectedRows = preparedStatement.executeUpdate();
            log.info("Запись {} удалена из базы", id);
            return affectedRows > 0;

        } catch (SQLException e) {
            log.error("Ошибка SQL запроса на удаление записи: ", e);
            return false;
        }
    }

    public Author getAuthor(int id) {
        String selectAuthorSQL = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = connection.prepareStatement(selectAuthorSQL)) {
            preparedStatement.setInt(1, id);
            log.debug("Выполнение SQL запроса на получение автора: {} с id: {}", selectAuthorSQL, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String name = resultSet.getString("name");
                int publishes = resultSet.getInt("publishesCount");
                int zeroCitiPublishes = resultSet.getInt("zeroCittPublishesCount");
                int hIndex = resultSet.getInt("hirshIndex");
                Author author = new Author(id, name, publishes, zeroCitiPublishes, hIndex);
                log.info("Из базы получен автор {}", author);
                return author;
            } else {
                log.debug("Автор с id {} не найден в базе данных", id);
            }
        } catch (SQLException e) {
            log.error("Ошибка SQL при запросе автора: ", e);
        }
        return null;
    }
}