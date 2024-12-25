package elibraryparser;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileService {

    public File chooseFile(Stage stage, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        return fileChooser.showOpenDialog(stage);
    }

    public File chooseSaveFile(Stage stage, String title, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Файлы", extension);
        fileChooser.getExtensionFilters().add(extFilter);
        return fileChooser.showSaveDialog(stage);
    }

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
}
