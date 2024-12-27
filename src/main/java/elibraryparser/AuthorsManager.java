package elibraryparser;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AuthorsManager {
    private final ElibraryParser parser;
    private final DatabaseManager database;

    public AuthorsManager(Map<String, String> config) {
        this.parser = new ElibraryParserRegex(false, config.getOrDefault("web_proxy", ""));
        this.database = new DatabaseManager();
    }

    public AuthorsManager(ElibraryParser parser, DatabaseManager database) {
        this.parser = parser;
        this.database = database;
    }

    public Set<Author> getAuthors(Set<Integer> authorIds) {
        return authorIds.stream()
                .map(authorId -> getAuthor(authorId))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    private Author getAuthor(int authorId) {
        if (database.recordExists(authorId)) {
            return database.getAuthor(authorId);
        }

        try {
            Author author = parser.getAuthor(authorId);
            if (author != null) {
                database.addAuthor(author);
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
        parser.close();
    }
}