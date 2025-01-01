package elibraryparser;

import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  Управляет процессом получения данных об авторах.
 *  Этот класс отвечает за получение информации об авторах из базы данных или с помощью парсера,
 *  а также за сохранение полученных данных в базу данных.
 */
@Log4j2
public class AuthorsManager {
    private final ElibraryParser parser;
    private final DatabaseManager database;

    /**
     * Конструктор для создания {@code AuthorsManager} с конфигурацией.
     *
     * @param config Карта параметров конфигурации, включая параметры прокси.
     *               Параметр "web_proxy" задает адрес прокси-сервера.
     */
    public AuthorsManager(Map<String, String> config) {
        log.info("Создание AuthorsManager с конфигурацией: {}", config);
        String webProxy = config.getOrDefault("web_proxy", "");
        this.parser = new ElibraryParserHttp(webProxy);
        this.database = new DatabaseManager();
        log.info("AuthorsManager создан");
    }

    /**
     * Конструктор для создания {@code AuthorsManager} с заданным парсером и менеджером базы данных.
     * Используется для тестирования или для внедрения зависимостей.
     *
     * @param parser   Экземпляр {@link ElibraryParser} для получения данных об авторах.
     * @param database Экземпляр {@link DatabaseManager} для работы с базой данных.
     */
    public AuthorsManager(ElibraryParser parser, DatabaseManager database) {
        log.info("Создание AuthorsManager с парсером: {} и базой данных: {}", parser, database);
        this.parser = parser;
        this.database = database;
        log.info("AuthorsManager создан");
    }

    /**
     * Получает данные о нескольких авторах.
     * Сначала проверяет наличие данных в базе данных, если нет, использует парсер для запроса данных.
     *
     * @param authorIds Набор идентификаторов авторов.
     * @return Набор объектов {@link Author}, представляющих найденных авторов.
     */
    public Set<Author> getAuthors(Set<Integer> authorIds) {
        log.info("Получение информации об авторах с ID: {}", authorIds);
        Set<Author> authors = authorIds.stream()
                .map(this::getAuthor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        log.info("Получена информация о {} авторах", authors.size());
        return authors;
    }

    /**
     * Получает данные об одном авторе.
     * Сначала проверяет наличие данных в базе данных, если нет, запрашивает данные через парсер.
     *
     * @param authorId Идентификатор автора.
     * @return Объект {@link Author} с данными об авторе, или {@code null}, если данные не найдены или произошла ошибка.
     */
    private Author getAuthor(int authorId) {
        log.debug("Получение информации об авторе с ID: {}", authorId);
        if (database.recordExists(authorId)) {
            Author author = database.getAuthor(authorId);
            log.debug("Автор {} найден в базе данных: {}", authorId, author);
            return author;
        }

        try {
            log.debug("Получение информации об авторе {} через парсер", authorId);
            Author author = parser.getAuthor(authorId);
            if (author != null) {
                database.addAuthor(author);
                log.debug("Информация об авторе {} сохранена в базе данных: {}", authorId, author);
                return author;
            } else {
                log.warn("Парсер вернул null для authorId: {}", authorId);
                return null;
            }
        } catch (RuntimeException e) {
            log.error("Ошибка при получении данных об авторе с ID " + authorId, e);
            return null;
        }
    }
}