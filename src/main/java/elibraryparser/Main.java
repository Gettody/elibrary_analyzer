package elibraryparser;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
//        // Создаем Playwright и запускаем браузер
//        String proxyUrl = "https://8.219.97.248:80";
//        try (Playwright playwright = Playwright.create()) {
//            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
//
//            //.setProxy(new Proxy(proxyUrl)
//            // Отключаем выполнение JavaScript и другие ресурсы
//            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
//                    .setJavaScriptEnabled(true)); // Отключаем JavaScript
//
//            // Создаем страницу
//            Page page = context.newPage();
//
//            // URL страницы
//            String url = "https://www.elibrary.ru/author_profile.asp?id=937895";
//
//            // Переход по URL с таймаутом 10 секунд
//            page.navigate(url, new Page.NavigateOptions().setTimeout(10000));
//
//            // Ожидаем, что страница станет "загруженной"
//            page.waitForLoadState(LoadState.NETWORKIDLE); // Можно использовать "LOAD" или "DOMCONTENTLOADED"
//
//            // Получаем HTML контент
//            String htmlContent = page.content();
//
//            // Выводим HTML
//            System.out.println(htmlContent);
//
//
//            // Закрытие браузера
//            browser.close();
//        ElibraryParser parser = new ElibraryParser();
//        List<Integer> authorIds = Arrays.asList(
//                937895
//        );
//
//        for (int authorId : authorIds) {
//            System.out.println("Processing author ID: " + authorId);
//            //String content = parser.getAuthorContent(authorId);
//            System.out.println(content);
//        }


        AuthorsManager authorsManager = new AuthorsManager();
        authorsManager.getAuthors(Set.of(937895, 715839, 592018, 956372, 625194)).forEach(author -> System.out.println(author.toString()));

    }
}



