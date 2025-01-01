package elibraryparser;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Log4j2
public class CommandLineApp {

    private static final String CONFIG_PATH = "analyzer.config";

    public static void main(String[] args) {
        System.setProperty("playwright.firefox.skipDownload", "true");
        System.setProperty("playwright.webkit.skipDownload", "true");

        Path authorsFilePath = null;
        Path outputFilePath = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-i")) {
                if (i + 1 < args.length) {
                    authorsFilePath = Paths.get(args[i + 1]);
                    i++; // Пропускаем следующий аргумент, так как это значение
                } else {
                    System.err.println("Ошибка: После '-i' должен быть указан путь к файлу с ID авторов.");
                    return;
                }
            } else if (args[i].equals("-o")) {
                if (i + 1 < args.length) {
                    outputFilePath = Paths.get(args[i + 1]);
                    i++; // Пропускаем следующий аргумент, так как это значение
                } else {
                    System.err.println("Ошибка: После '-o' должен быть указан путь для сохранения результатов.");
                    return;
                }
            } else {
                System.err.println("Ошибка: Неизвестный аргумент командной строки: " + args[i]);
                return;
            }
        }

        if (authorsFilePath == null || outputFilePath == null) {
            System.err.println("Ошибка: Необходимо указать пути к файлу с ID авторов (-i) и путь для сохранения результатов (-o).");
            return;
        }

        if (!authorsFilePath.toFile().exists() || authorsFilePath.toFile().isDirectory()) {
            System.err.println("Ошибка: Файл с ID авторов не существует или является директорией: " + authorsFilePath);
            return;
        }

        File outputFile = outputFilePath.toFile();
        if (outputFile.exists() && !outputFile.isFile()) {
            System.err.println("Ошибка: Указанный путь для сохранения результатов не является файлом: " + outputFilePath);
            return;
        }
        if (!outputFile.exists() && outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            System.err.println("Ошибка: Родительская директория для сохранения результатов не существует: " + outputFile.getParentFile());
            return;
        }

        log.info("Путь к файлу с ID авторов: {}", authorsFilePath);
        log.info("Путь для сохранения результатов: {}", outputFilePath);

        AuthorsManager authorsManager = new AuthorsManager(FileService.readConfigFile(CONFIG_PATH));
        FileService fileService = new FileService();

        try {
            Set<Integer> authorIds = fileService.getAuthorIdsFromFile(authorsFilePath);
            log.info("Загружено {} ID авторов.", authorIds.size());

            if (!authorIds.isEmpty()) {
                log.info("Начало обработки авторов...");
                Set<Author> authors = authorsManager.getAuthors(authorIds);
                log.info("Обработано {} авторов.", authors.size());

                fileService.saveAuthorsToMarkdown(authors, outputFilePath);
                log.info("Результаты сохранены в: {}", outputFilePath);
            } else {
                log.warn("Файл с ID авторов пуст, обработка не требуется.");
            }

        } catch (IOException e) {
            log.error("Ошибка при обработке файлов: {}", e.getMessage());
            System.err.println("Ошибка при обработке файлов: " + e.getMessage());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка: {}", e.getMessage(), e);
            System.err.println("Непредвиденная ошибка: " + e.getMessage());
        }
    }
}
