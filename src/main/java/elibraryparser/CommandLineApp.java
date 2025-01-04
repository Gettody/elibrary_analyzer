package elibraryparser;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Консольное приложение для обработки данных об авторах из файла.
 * <p>
 * Приложение считывает идентификаторы авторов из указанного входного файла,
 * получает информацию об этих авторах и сохраняет результаты в выходной файл.
 */
@Log4j2
public class CommandLineApp {
    private static final String CONFIG_PATH = "analyzer.config";

    /**
     * Главный метод консольного приложения.
     * <p>
     * Получает пути к входному и выходному файлам из аргументов командной строки,
     * считывает идентификаторы авторов, получает информацию об авторах и сохраняет результаты.
     *
     * @param args Массив строк, содержащий путь к файлу с ID авторов (args[0])
     *             и путь к файлу для сохранения результатов (args[1]).
     */
    public static void main(String[] args) {
        Path authorsFilePath = Paths.get(args[0]);
        Path outputFilePath = Paths.get(args[1]);

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
            log.error("Ошибка ввода/вывода при обработке файлов: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка: {}", e.getMessage(), e);
        }
    }
}