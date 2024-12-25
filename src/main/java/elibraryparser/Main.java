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

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class Main extends Application {

    private final AuthorsManager authorsManager = new AuthorsManager();
    private final FileService fileService = new FileService();
    private Set<Integer> authorIds;
    private Set<Author> authors;
    private File saveFile;
    private Button processAuthorsButton;
    private Button chooseSaveFileButton;
    private Label statusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Elibrary Parser");

        Button loadAuthorsButton = new Button("Загрузить ID авторов из файла");
        processAuthorsButton = new Button("Начать обработку авторов");
        chooseSaveFileButton = new Button("Выбрать файл для сохранения результатов");
        statusLabel = new Label("");

        processAuthorsButton.setDisable(true);
        chooseSaveFileButton.setDisable(true);

        loadAuthorsButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл с ID авторов");
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                try {
                    authorIds = fileService.getAuthorIdsFromFile(selectedFile.toPath());
                    statusLabel.setText("ID авторов загружены. Нажмите 'Начать обработку авторов'.");
                    processAuthorsButton.setDisable(false);
                } catch (IOException e) {
                    statusLabel.setText("Ошибка чтения файла: " + e.getMessage());
                    processAuthorsButton.setDisable(true);
                }
            } else {
                statusLabel.setText("Файл не выбран.");
                processAuthorsButton.setDisable(true);
            }
            chooseSaveFileButton.setDisable(true);
        });

        processAuthorsButton.setOnAction(event -> {
            if (authorIds != null && !authorIds.isEmpty()) {
                statusLabel.setText("Идет обработка авторов...");
                processAuthorsButton.setDisable(true); // Disable during processing

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
                });

                fetchAuthorsTask.setOnFailed(e -> {
                    statusLabel.setText("Ошибка при обработке авторов: " + fetchAuthorsTask.getException().getMessage());
                    chooseSaveFileButton.setDisable(true);
                    processAuthorsButton.setDisable(false); // Re-enable in case of failure
                });

                new Thread(fetchAuthorsTask).start();
            } else {
                statusLabel.setText("Сначала загрузите ID авторов.");
            }
            chooseSaveFileButton.setDisable(true);
        });

        chooseSaveFileButton.setOnAction(event -> {
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
                    } catch (IOException e) {
                        statusLabel.setText("Ошибка сохранения файла: " + e.getMessage());
                    }
                } else {
                    statusLabel.setText("Файл для сохранения не выбран.");
                }
            } else {
                statusLabel.setText("Сначала обработайте авторов.");
            }
        });

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(loadAuthorsButton, processAuthorsButton, chooseSaveFileButton, statusLabel);

        Scene scene = new Scene(layout, 450, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}