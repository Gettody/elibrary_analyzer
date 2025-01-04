package elibraryparser;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

import java.util.concurrent.Callable;

/**
 *  Точка входа в приложение.
 *  Этот класс использует picocli для обработки аргументов командной строки. Он может запустить либо графическое
 *  приложение, либо приложение командной строки в зависимости от предоставленных аргументов.
 */
@Command(name = "elibrary_analyzer", mixinStandardHelpOptions = true, version = "v1.0", description = "Приложение анализа публикационной деятельности авторов на elibrary.ru")
public class Main implements Callable<Integer> {

    GuiApp gui = new GuiApp();
    CommandLineApp cli = new CommandLineApp();


    @Option(names = "--no-graphics", description = "Запускает программу в режиме без графики.")
    private boolean noGraphics;

    @Option(names = {"-i", "--input"}, description = "Входной файл с authorsId для режима без графики (расширение .txt)")
    private String input;

    @Option(names = {"-o", "--output"}, description = "Выходной файл для режима без графики (расширение .md)")
    private String output;

    /**
     *  Основная логика приложения. Вызывается при запуске приложения.
     *  Проверяет наличие опции "--no-graphics" и запускает либо графическое приложение, либо приложение командной строки
     *  соответственно.
     *  @return Целочисленный код выхода (0 для успеха).
     *  @throws Exception если во время выполнения возникает ошибка.
     */
    @Override
    public Integer call() throws Exception {
        if (noGraphics) {
            if (input == null || output == null) {
                throw new ParameterException(new CommandLine(this),
                        "Опции -i и -o обязательны при использовании --no-graphics.");
            }
            cli.main(new String[]{input, output});
        } else {
            gui.main(new String[0]);
        }
        return 0;
    }

    /**
     *  Главный метод приложения. Это первая точка входа при запуске приложения.
     *  Он использует picocli для разбора аргументов командной строки, затем вызывает метод {@link #call()}.
     *  @param args Аргументы командной строки, переданные приложению.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}