package elibraryparser;

import com.microsoft.playwright.*;

import java.util.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElibraryParserRegex implements ElibraryParser {

    private Playwright playwright;
    private Browser browser;
    private Page page;
    private static final Integer MIN_DELAY = 2000;
    private static final Integer MAX_DELAY = 3000;
    private static final String BASE_URL = "https://app.scrapingbee.com/api/v1/?api_key=UQ4XVODPXTF1OJW9JUE297JYEVUQTFDQ1GO5BC6HBGQPER7HJJBNWHHVMYWHF0UOXLYA5GKLWA6Q9TKN&url=https://www.elibrary.ru/author_profile.asp?authorid=";

    public ElibraryParserRegex() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public ElibraryParserRegex(boolean headless) {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }

    @Override
    public Author getAuthor(int authorId) {
        Map<String, String> authorData = scrapeAuthorData(String.valueOf(authorId));
        if (authorData == null || authorData.containsValue(null)) {
            return null;
        }
        try {
            String name = authorData.get("ФИО");
            int publishesCount = Integer.parseInt(authorData.get("Число публикаций").replaceAll("[^0-9]", ""));
            int citedPublishesCount = Integer.parseInt(authorData.get("Число цитирований").replaceAll("[^0-9]", ""));
            int hirshIndex = Integer.parseInt(authorData.get("Индекс Хирша"));
            int zeroCittPublishesCount = publishesCount - citedPublishesCount;
            return new Author(authorId, name, publishesCount, zeroCittPublishesCount, hirshIndex);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка при парсинге числовых значений для автора " + authorId + ": " + e.getMessage());
            return null;
        }
    }

    public Map<String, String> scrapeAuthorData(String authorId) {
        String url = BASE_URL + authorId;
        String pageContent = downloadPage(url);
        System.out.println(pageContent);
        if (pageContent != null) {
            return extractData(pageContent);
        } else {
            return null;
        }
    }

    private String downloadPage(String url) {
        try {
            page = browser.newPage(new Browser.NewPageOptions().setUserAgent(getRandomUserAgent()));

            page.onResponse(response -> {
                if (response.status() == 500) {
                    throw new PlaywrightException(String.format("Автор с authorId: %s не найден", url));
                }
            });

            page.navigate(url);
            randomDelay(MIN_DELAY, MAX_DELAY);
            return page.content();

        } catch (PlaywrightException e) {
            System.err.println("Ошибка при загрузке страницы с помощью Playwright: " + e.getMessage());
            return null;
        } finally {
            if (page != null) {
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
            } else {
                System.out.println("Не удалось найти " + fieldName + " по regex: " + regex);
                data.put(fieldName, null);
            }
        }

        return data;
    }

    private static void randomDelay(int min, int max) {
        Random random = new Random();
        int delay = random.nextInt(min, max);
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
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
        return userAgents.get(random.nextInt(userAgents.size()));
    }

    public void close() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}