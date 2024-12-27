package elibraryparser;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class FileService {
    public Set<Integer> getAuthorIdsFromFile(Path filePath) throws IOException {
        String fileContent = Files.readString(filePath);
        return parseAuthorIds(fileContent);
    }

    public Set<Integer> parseAuthorIds(String text) {
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        System.err.println("Некорректный ID автора: " + s);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void saveAuthorsToMarkdown(Set<Author> authors, Path filePath) throws IOException {
        List<String> markdownLines = generateMarkdownTable(authors);
        Files.write(filePath, markdownLines);
    }

    private List<String> generateMarkdownTable(Set<Author> authors) {
        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder.append("|ID|ФИО|Статей|Статей без цитирования|Индекс Хирша|\n");
        tableBuilder.append("|---|---|---|---|---|\n");

        for (Author author : authors) {
            tableBuilder.append(String.format("|%d|%s|%d|%d|%d|\n",
                    author.authorId(), author.name(), author.publishesCount(), author.zeroCittPublishesCount(), author.hirshIndex()));
        }
        return List.of(tableBuilder.toString());
    }

    public static Map<String, String> readConfigFile(String filePath) {
        try {
            return Files.lines(Paths.get(filePath))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                            parts -> parts[0].trim(),
                            parts -> parts[1].trim()
                    ));
        } catch (IOException e) {
            log.error("Ошибка чтения конфигурационного файла: {}", filePath, e);
            return Collections.emptyMap();
        }
    }
}
