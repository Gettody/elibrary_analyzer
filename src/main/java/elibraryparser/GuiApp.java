package elibraryparser;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 *  Основной класс JavaFX приложения для парсинга данных об авторах с eLibrary.ru.
 *  Этот класс управляет пользовательским интерфейсом, загрузкой данных об авторах, их обработкой и сохранением результатов.
 */
@Log4j2
public class GuiApp extends Application {
    private static final String CONFIG_PATH = "analyzer.config";
    private final AuthorsManager authorsManager = new AuthorsManager(FileService.readConfigFile(CONFIG_PATH));
    private final FileService fileService = new FileService();
    private Set<Integer> authorIds;
    private Set<Author> authors;
    private Button processAuthorsButton;
    private Button chooseSaveFileButton;
    private Label statusLabel;

    /**
     *  Главный метод приложения.
     *  Запускает JavaFX приложение. Устанавливает системные свойства для Playwright, чтобы избежать скачивания браузеров.
     *
     * @param args Аргументы командной строки.
     */
    public static void main(String[] args) {
        System.setProperty("playwright.firefox.skipDownload", "true");
        System.setProperty("playwright.webkit.skipDownload", "true");
        launch(args);
    }

    /**
     *  Инициализирует и отображает основное окно приложения.
     *  Создает кнопки для загрузки ID авторов, запуска процесса обработки и выбора файла для сохранения.
     *  Устанавливает обработчики событий для каждой кнопки.
     *
     *  @param primaryStage Основной Stage приложения.
     */
    @Override
    public void start(Stage primaryStage) {
        log.info("Запуск приложения");
        primaryStage.setTitle("Elibrary Parser");

        Button loadAuthorsButton = new Button("Загрузить ID авторов из файла");
        processAuthorsButton = new Button("Начать обработку авторов");
        chooseSaveFileButton = new Button("Выбрать файл для сохранения результатов");
        statusLabel = new Label("");

        processAuthorsButton.setDisable(true);
        chooseSaveFileButton.setDisable(true);

        loadAuthorsButton.setOnAction(event -> {
            log.info("Нажата кнопка 'Загрузить ID авторов из файла'");
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл с ID авторов");
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                try {
                    authorIds = fileService.getAuthorIdsFromFile(selectedFile.toPath());
                    statusLabel.setText("ID авторов загружены. Нажмите 'Начать обработку авторов'.");
                    processAuthorsButton.setDisable(false);
                    log.info("Файл с ID авторов выбран: {}", selectedFile.getAbsolutePath());
                } catch (IOException e) {
                    statusLabel.setText("Ошибка чтения файла: " + e.getMessage());
                    processAuthorsButton.setDisable(true);
                    log.error("Ошибка чтения файла: {}", selectedFile.getAbsolutePath(), e);
                }
            } else {
                statusLabel.setText("Файл не выбран.");
                processAuthorsButton.setDisable(true);
                log.info("Выбор файла отменен");
            }
            chooseSaveFileButton.setDisable(true);
        });

        processAuthorsButton.setOnAction(event -> {
            log.info("Нажата кнопка 'Начать обработку авторов'");
            if (authorIds != null && !authorIds.isEmpty()) {
                statusLabel.setText("Идет обработка авторов...");
                processAuthorsButton.setDisable(true);

                Task<Set<Author>> fetchAuthorsTask = new Task<>() {
                    @Override
                    protected Set<Author> call() throws Exception {
                        return authorsManager.getAuthors(authorIds);
                    }
                };

                fetchAuthorsTask.setOnSucceeded(e -> {
                    authors = fetchAuthorsTask.getValue();
                    statusLabel.setText("Обработка авторов завершена. Готов к сохранению.");
                    chooseSaveFileButton.setDisable(false);
                    log.info("Обработка авторов завершена, получено {} авторов", authors.size());
                });

                fetchAuthorsTask.setOnFailed(e -> {
                    statusLabel.setText("Ошибка при обработке авторов: " + fetchAuthorsTask.getException().getMessage());
                    chooseSaveFileButton.setDisable(true);
                    processAuthorsButton.setDisable(false); // Re-enable in case of failure
                    log.error("Ошибка при обработке авторов", fetchAuthorsTask.getException());
                });

                new Thread(fetchAuthorsTask).start();
            } else {
                statusLabel.setText("Сначала загрузите ID авторов.");
                log.warn("Попытка обработки авторов без загруженных ID");
            }
            chooseSaveFileButton.setDisable(true);
        });

        chooseSaveFileButton.setOnAction(event -> {
            log.info("Нажата кнопка 'Выбрать файл для сохранения результатов'");
            if (authors != null && !authors.isEmpty()) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Выберите файл для сохранения");
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Markdown files (*.md)", "*.md");
                fileChooser.getExtensionFilters().add(extFilter);
                File selectedSaveFile = fileChooser.showSaveDialog(primaryStage);
                if (selectedSaveFile != null) {
                    try {
                        fileService.saveAuthorsToMarkdown(authors, selectedSaveFile.toPath());
                        statusLabel.setText("Результаты сохранены в: " + selectedSaveFile.getAbsolutePath());
                        log.info("Результаты сохранены в файл: {}", selectedSaveFile.getAbsolutePath());
                    } catch (IOException e) {
                        statusLabel.setText("Ошибка сохранения файла: " + e.getMessage());
                        log.error("Ошибка сохранения файла: {}", selectedSaveFile.getAbsolutePath(), e);
                    }
                } else {
                    statusLabel.setText("Файл для сохранения не выбран.");
                    log.info("Выбор файла для сохранения отменен");
                }
            } else {
                statusLabel.setText("Сначала обработайте авторов.");
                log.warn("Попытка сохранения результатов без обработанных авторов");
            }
        });

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(loadAuthorsButton, processAuthorsButton, chooseSaveFileButton, statusLabel);

        Scene scene = new Scene(layout, 450, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
        log.info("Отображение главного окна приложения");
    }

    /**
     *  Выполняет действия при остановке приложения.
     *
     *  @throws Exception Если во время остановки приложения произошла ошибка.
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        log.info("Приложение остановлено");
    }
}