package elibraryparser;

import lombok.extern.log4j.Log4j2;

import java.sql.*;

/**
 *  Управляет операциями с базой данных для хранения и извлечения информации об авторах.
 *  Этот класс предоставляет методы для создания таблиц, добавления, удаления и получения записей об авторах.
 */
@Log4j2
public class DatabaseManager {

    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:authors.db";
    private final String databaseUrl;
    private static final String TABLE_NAME = "authors";

    /**
     * Создает экземпляр {@code DatabaseManager} с URL базы данных по умолчанию.
     * Инициализирует базу данных и создает таблицу, если она не существует.
     */
    public DatabaseManager() {
        this(DEFAULT_DATABASE_URL);
    }

    /**
     * Создает экземпляр {@code DatabaseManager} с указанным URL базы данных.
     * Инициализирует базу данных и создает таблицу, если она не существует.
     * @param databaseUrl URL базы данных для подключения.
     */
    public DatabaseManager(String databaseUrl) {
        this.databaseUrl = databaseUrl;
        log.info("Инициализация DatabaseManager с URL: {}", databaseUrl);
        createTableIfNotExists();
        log.info("DatabaseManager инициализирован");
    }

    /**
     * Создает таблицу авторов, если она не существует в базе данных.
     */
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

    /**
     * Выполняет заданный SQL-запрос.
     * @param sql SQL-запрос для выполнения.
     */
    private void executeStatement(String sql) {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            log.debug("Выполнение SQL запроса: {}", sql);
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.error("SQL ошибка при запросе: ", e);
        }
    }

    /**
     * Выполняет SQL-запрос для проверки существования записи с указанным ID.
     * @param sql SQL-запрос для выполнения.
     * @param id ID для проверки.
     * @return {@code true}, если запись существует с указанным ID, {@code false} в противном случае.
     */
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

    /**
     * Проверяет, существует ли запись в базе данных с указанным ID.
     * @param id ID записи для проверки.
     * @return {@code true}, если запись существует с указанным ID, {@code false} в противном случае.
     */
    public boolean recordExists(int id) {
        String checkRecordSQL = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";
        return executeQueryForExists(checkRecordSQL, id);
    }

    /**
     * Добавляет запись об авторе в базу данных.
     * @param author Объект автора для добавления в базу данных.
     * @return {@code true}, если автор успешно добавлен в базу данных, {@code false} в противном случае.
     */
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

    /**
     * Добавляет запись в базу данных с указанными данными об авторе.
     * @param id ID автора.
     * @param name Имя автора.
     * @param publishes Количество публикаций автора.
     * @param zeroCittPublishesCount Количество публикаций без цитирований.
     * @param hIndex Индекс Хирша автора.
     * @return {@code true}, если запись успешно добавлена в базу данных, {@code false} в противном случае.
     */
    private boolean addRecord(int id, String name, int publishes, int zeroCittPublishesCount, int hIndex) {
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
            log.debug("Выполнение SQL запроса на добавление записи: {} с параметрами: id={}, name={}, publishes={}, zeroCitt={}, hIndex={}",
                    insertRecordSQL, id, name, publishes, zeroCittPublishesCount, hIndex);
            preparedStatement.executeUpdate();
            log.info("Запись {} добавлена", id);
            return true;
        } catch (SQLException e) {
            log.error("SQL ошибка при добавлении записи: ", e);
            return false;
        }
    }

    /**
     * Удаляет запись из базы данных с указанным ID.
     * @param id ID записи для удаления.
     * @return {@code true}, если запись успешно удалена из базы данных, {@code false} в противном случае.
     */
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

    /**
     * Получает автора из базы данных с указанным ID.
     * @param id ID автора для получения.
     * @return Объект {@link Author}, представляющий автора, или {@code null}, если автор не найден.
     */
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