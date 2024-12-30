package elibraryparser;

import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class AuthorsManager {
    private final ElibraryParser parser;
    private final DatabaseManager database;

    public AuthorsManager(Map<String, String> config) {
        log.info("Создание AuthorsManager с конфигурацией: {}", config);
        String webProxy = config.getOrDefault("web_proxy", "");
        boolean headless = true;
        String headlessStr = config.get("headless");
        if (headlessStr != null) {
            try {
                headless = Boolean.parseBoolean(headlessStr.toLowerCase());
                log.debug("Конфигурация headless: {}", headless);
            } catch (IllegalArgumentException e) {
                log.warn("Не удалось получить значение 'headless' из конфигурации: {}. Используется значение по умолчанию: false", headlessStr);
            }
        }
        this.parser = new ElibraryParserRegex(headless, webProxy);
        this.database = new DatabaseManager();
        log.info("AuthorsManager создан");
    }

    public AuthorsManager(ElibraryParser parser, DatabaseManager database) {
        log.info("Создание AuthorsManager с парсером: {} и базой данных: {}", parser, database);
        this.parser = parser;
        this.database = database;
        log.info("AuthorsManager создан");
    }

    public Set<Author> getAuthors(Set<Integer> authorIds) {
        log.info("Получение информации об авторах с ID: {}", authorIds);
        Set<Author> authors = authorIds.stream()
                .map(this::getAuthor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        log.info("Получена информация о {} авторах", authors.size());
        return authors;
    }

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

    public void closeParser() {
        log.info("Закрытие парсера");
        parser.close();
        log.info("Парсер закрыт");
    }
}