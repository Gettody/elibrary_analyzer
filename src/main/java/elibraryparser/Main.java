package elibraryparser;

import java.util.Arrays;
import java.util.Set;

/**
 * Точка входа в приложение.
 * Этот класс содержит метод main, который определяет, какое приложение запустить:
 * графическое приложение {@link GuiApp} или консольное приложение {@link CommandLineApp}.
 * Выбор зависит от наличия аргумента командной строки "--no-graphics".
 */
public class Main {
    /**
     * Главный метод приложения.
     * Определяет, какое приложение запустить, на основе аргументов командной строки.
     * Если присутствует аргумент "--no-graphics", запускается консольное приложение {@link CommandLineApp}.
     * В противном случае запускается графическое приложение {@link GuiApp}.
     *
     * @param args Аргументы командной строки. Если содержит "--no-graphics", запускается консольное приложение.
     */
    public static void main(String[] args) {
        if (Set.of(args).contains("--no-graphics")) {
            String[] commandLineArgs = Arrays.stream(args)
                    .filter(arg -> !arg.equals("--no-graphics"))
                    .toArray(String[]::new);
            CommandLineApp.main(commandLineArgs);
        } else {
            GuiApp.main(args);
        }
    }
}