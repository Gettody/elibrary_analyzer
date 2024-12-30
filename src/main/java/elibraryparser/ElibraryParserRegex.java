package elibraryparser;

import com.microsoft.playwright.*;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class ElibraryParserRegex implements ElibraryParser {

    private Playwright playwright;
    private Browser browser;
    private Page page;
    private static final Integer MIN_DELAY = 2000;
    private static final Integer MAX_DELAY = 3000;
    private static final String BASE_URL = "https://www.elibrary.ru/author_profile.asp?authorid=";
    private static String webProxyUrl;

    public ElibraryParserRegex() {
        log.info("Инициализация ElibraryParserRegex без параметров");
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            log.info("Playwright и Browser инициализированы");
        } catch (Exception e) {
            log.error("Ошибка при инициализации Playwright или Browser", e);
        }
    }

    public ElibraryParserRegex(boolean headless, String webProxyUrl) {
        log.info("Инициализация ElibraryParserRegex с headless: {} и webProxyUrl: {}", headless, webProxyUrl);
        ElibraryParserRegex.webProxyUrl = webProxyUrl;
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        log.info("Playwright и Browser инициализированы");
    }

    @Override
    public Author getAuthor(int authorId) {
        log.info("Получение информации об авторе с ID: {}", authorId);
        Map<String, String> authorData = scrapeAuthorData(String.valueOf(authorId));
        if (authorData == null || authorData.containsValue(null)) {
            log.warn("Не удалось получить данные об авторе с ID: {}", authorId);
            return null;
        }
        try {
            String name = authorData.get("ФИО");
            int publishesCount = Integer.parseInt(authorData.get("Число публикаций").replaceAll("[^0-9]", ""));
            int citedPublishesCount = Integer.parseInt(authorData.get("Число цитирований").replaceAll("[^0-9]", ""));
            int hirshIndex = Integer.parseInt(authorData.get("Индекс Хирша"));
            int zeroCittPublishesCount = publishesCount - citedPublishesCount;
            Author author = new Author(authorId, name, publishesCount, zeroCittPublishesCount, hirshIndex);
            log.info("Информация об авторе {} успешно получена: {}", authorId, author);
            return author;
        } catch (NumberFormatException e) {
            log.error("Ошибка при парсинге числовых значений для автора " + authorId + ": " + e.getMessage());
            return null;
        }
    }

    public Map<String, String> scrapeAuthorData(String authorId) {
        String url = webProxyUrl + BASE_URL + authorId;
        log.info("Загрузка страницы автора с ID: {} по URL: {}", authorId, url);
        String pageContent = downloadPage(url);
        if (pageContent != null) {
            Map<String, String> data = extractData(pageContent);
            log.debug("Данные автора {} успешно извлечены: {}", authorId, data);
            return data;
        } else {
            log.warn("Не удалось загрузить страницу для автора с ID: {}", authorId);
            return null;
        }
    }

    private String downloadPage(String url) {
        try {
            log.debug("Открытие новой страницы браузера");
            page = browser.newPage(new Browser.NewPageOptions().setUserAgent(getRandomUserAgent()));

            page.onResponse(response -> {
                if (response.status() == 500) {
                    log.error("Сервер вернул ошибку 500 для URL: {}", response.url());
                    throw new PlaywrightException(String.format("Автор по URL: %s не найден", url));
                }
            });

            log.debug("Навигация к URL: {}", url);
            page.navigate(url);
            randomDelay(MIN_DELAY, MAX_DELAY);
            String content = page.content();
            log.debug("Страница успешно загружена");
            return content;

        } catch (PlaywrightException e) {
            log.error("Ошибка Playwright при загрузке страницы: " + e.getMessage());
            return null;
        } finally {
            if (page != null) {
                log.debug("Закрытие страницы");
                page.close();
            }
        }
    }

    private Map<String, String> extractData(String pageContent) {
        Map<String, String> data = new HashMap<>();

        // Регулярные выражения и их описания
        Map<String, String> regexMap = new HashMap<>();
        regexMap.put("title>(.+?) - Анализ публикационной активности</title>", "ФИО");
        regexMap.put(">Число публикаций на elibrary.ru<\\/font><\\/td><td align=\"center\" class=\"midtext\"><font color=\"#000000\"><a[^>]*>(\\d+)<\\/a>", "Число публикаций");
        regexMap.put(">Индекс Хирша по всем публикациям на elibrary.ru<\\/font><\\/td><td align=\"center\" class=\"midtext\"><font color=\"#000000\">(\\d+)<\\/font>", "Индекс Хирша");
        regexMap.put(">Число публикаций автора, процитированных хотя бы один раз<\\/font><\\/td><td align=\"center\" class=\"midtext\"><font color=\"#000000\">(\\d+)[^<]+<\\/font>", "Число цитирований");

        for (Map.Entry<String, String> entry : regexMap.entrySet()) {
            String regex = entry.getKey();
            String fieldName = entry.getValue();

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(pageContent);

            if (matcher.find()) {
                data.put(fieldName, matcher.group(1).trim());
                log.debug("Извлечено значение '{}': '{}' по regex: '{}'", fieldName, matcher.group(1).trim(), regex);
            } else {
                log.warn("Не удалось найти '{}' по regex: '{}'", fieldName, regex);
                data.put(fieldName, null);
            }
        }

        return data;
    }

    private static void randomDelay(int min, int max) {
        Random random = new Random();
        int delay = random.nextInt(min, max);
        log.debug("Применение случайной задержки: {} мс", delay);
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            log.warn("Задержка была прервана", e);
            Thread.currentThread().interrupt();
        }
    }

    private static String getRandomUserAgent() {
        List<String> userAgents = Arrays.asList(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/123.0.2420.53 Safari/537.36"
        );
        Random random = new Random();
        String userAgent = userAgents.get(random.nextInt(userAgents.size()));
        log.debug("Выбран случайный User-Agent: {}", userAgent);
        return userAgent;
    }

    @Override
    public void close() {
        log.info("Закрытие ElibraryParserRegex");
        if (browser != null) {
            log.debug("Закрытие браузера");
            browser.close();
        }
        if (playwright != null) {
            log.debug("Закрытие Playwright");
            playwright.close();
        }
        log.info("ElibraryParserRegex закрыт");
    }
}