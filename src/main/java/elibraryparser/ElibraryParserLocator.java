package elibraryparser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.Page;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ElibraryParserLocator {
    private Playwright playwright;
    private final Browser browser;
    private static final Integer MIN_DELAY = 1000;
    private static final Integer MAX_DELAY = 3000;
    private static final int MAX_WAIT_TIME_SEC = 30;
    private static final String BASE_URL = "https://www.elibrary.ru/author_profile.asp?id=";
    private static final Map<String, String> SELECTORS = new HashMap<String, String>();

    public ElibraryParserLocator() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        SELECTORS.put(
                "authorName",
                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(2) > td:nth-child(1) > table:nth-child(1) > tbody > tr > td > div > font:nth-child(1) > b"
        );
        SELECTORS.put(
                "publishesCount",
                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(2) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(4) > td:nth-child(3) > font > a"
        );
        SELECTORS.put(
                "citedPublishesCount",
                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(2) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(18) > td:nth-child(3) > font"
        );
        SELECTORS.put(
                "hirshIndex",
                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(2) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(12) > td:nth-child(3) > font"
        );
    }

    public ElibraryParserLocator(Map<String, String> customSelectors) {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        SELECTORS.putAll(customSelectors);
    }

    private Map<String, String> getAuthorContent(int id) {
        randomDelay(MIN_DELAY, MAX_DELAY);
        try (Page page = browser.newPage(new Browser.NewPageOptions().setUserAgent(getRandomUserAgent()))) {
            final String navigatedUrl = BASE_URL + id;

            page.onResponse(response -> {
                if (response.status() == 500) {
                    throw new PlaywrightException(String.format("Автор с authorId: %d в системе РИЦН не существует!", id));
                }
            });

            page.navigate(navigatedUrl);
            waitForCorrectUrl(page,navigatedUrl);
            Map<String, String> results = SELECTORS.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                //randomDelay(MIN_DELAY, MAX_DELAY);
                                try {
                                    Locator locator = page.locator(entry.getValue());
                                    System.out.println(locator.innerText());
                                    return locator.innerText();
                                } catch (PlaywrightException e) {
                                    System.err.println("Ошибка при извлечении данных для селектора: " + entry.getValue() + " на странице: " + navigatedUrl);
                                    return "0";
                                }
                            }
                    ));
            return results;
        } catch (PlaywrightException e) {
            System.out.println("Произошла ошибка при запросе или извлечении данных для автора " + id + ": " + e.getMessage());
            return null;
        }
    }

    private void waitForCorrectUrl(Page page, String expectedUrl) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(MAX_WAIT_TIME_SEC)) {
            String currentUrl = page.url();
            if (currentUrl.startsWith(expectedUrl)) {
                return;
            }
            randomDelay(1000,2000);
        }
        throw new RuntimeException(String.format("Превышено время ожидания перенаправления на корректный URL %s", expectedUrl));
    }

    public Author getAuthor(int id) {
        Map<String, String> authorContent = getAuthorContent(id);
        try {
            String authorName = authorContent.get("authorName");
            int publishesCount = Integer.parseInt(authorContent.get("publishesCount").replaceAll("[^0-9]", ""));
            int citedPublishesCount = publishesCount - Integer.parseInt(authorContent.get("citedPublishesCount").split(" ")[0]);
            int hirshIndex =  Integer.parseInt(authorContent.get("hirshIndex"));
            return new Author(id, authorName, publishesCount, citedPublishesCount, hirshIndex);
        } catch (RuntimeException e) {
            throw new RuntimeException("Ошибка при создании объекта Author: ошибка парсинга Integer", e);
        }
    }


    private static void randomDelay(int min, int max) {
        Random random = new Random();
        int delay = random.nextInt(min, max);
        try {
            Thread.sleep(delay);
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
}