package elibraryparser;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class FileService {
    public Set<Integer> getAuthorIdsFromFile(Path filePath) throws IOException {
        log.info("Чтение ID авторов из файла: {}", filePath);
        String fileContent = Files.readString(filePath);
        Set<Integer> authorIds = parseAuthorIds(fileContent);
        log.info("Прочитано {} ID авторов из файла: {}", authorIds.size(), filePath);
        return authorIds;
    }

    public Set<Integer> parseAuthorIds(String text) {
        log.debug("Поиск authorId в тексте: {}", text);
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        int id = Integer.parseInt(s);
                        log.debug("Успешно спарсен ID автора: {}", id);
                        return id;
                    } catch (NumberFormatException e) {
                        log.error("Некорректный ID автора: {}", s);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void saveAuthorsToMarkdown(Set<Author> authors, Path filePath) throws IOException {
        log.info("Сохранение информации об авторах в Markdown файл: {}", filePath);
        List<String> markdownLines = generateMarkdownTable(authors);
        Files.write(filePath, markdownLines);
        log.info("Информация о {} авторах сохранена в файл: {}", authors.size(), filePath);
    }

    private List<String> generateMarkdownTable(Set<Author> authors) {
        log.debug("Генерация Markdown таблицы для {} авторов", authors.size());
        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder.append("|ID|ФИО|Статей|Статей без цитирования|Индекс Хирша|\n");
        tableBuilder.append("|---|---|---|---|---|\n");

        for (Author author : authors) {
            tableBuilder.append(String.format("|%d|%s|%d|%d|%d|\n",
                    author.authorId(), author.name(), author.publishesCount(), author.zeroCittPublishesCount(), author.hirshIndex()));
        }
        log.debug("Markdown таблица сгенерирована:\n{}", tableBuilder);
        return List.of(tableBuilder.toString());
    }

    public static Map<String, String> readConfigFile(String filePath) {
        log.info("Чтение конфигурационного файла: {}", filePath);
        try {
            Map<String, String> config = Files.lines(Paths.get(filePath))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                            parts -> parts[0].trim(),
                            parts -> parts[1].trim()
                    ));
            log.info("Конфигурационный файл {} успешно прочитан. Параметры: {}", filePath, config);
            return config;
        } catch (IOException e) {
            log.error("Ошибка чтения конфигурационного файла: {}", filePath, e);
            return Collections.emptyMap();
        }
    }
}