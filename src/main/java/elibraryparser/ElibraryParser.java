package elibraryparser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.Page;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ElibraryParser {
    private Playwright playwright;
    private final Browser browser;
    private static final Integer MIN_DELAY = 1000;
    private static final Integer MAX_DELAY = 3000;
    private static final Map<String, String> SELECTORS = new HashMap<String, String>();

    public ElibraryParser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        SELECTORS.put(
                "authorName",
                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(3) > td:nth-child(1) > table:nth-child(1) > tbody > tr > td > div > font:nth-child(1) > b"
        );
        SELECTORS.put(
                "publishesCount",
                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(3) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(4) > td:nth-child(3) > font > a"
        );
        SELECTORS.put(
                "noZeroCittPublishes",
                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(3) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(18) > td:nth-child(3) > font"
        );
        SELECTORS.put(
                "hirshIndex",
                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(3) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(12) > td:nth-child(3) > font"
        );
    }

    public ElibraryParser(Map<String, String> customSelectors) {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        SELECTORS.putAll(customSelectors);
    }


    private List<String> getAuthorContent(int id) {
        randomDelay(MIN_DELAY, MAX_DELAY);
        Page page = browser.newPage(new Browser.NewPageOptions().setUserAgent(getRandomUserAgent()));
        page.navigate("https://www.elibrary.ru/author_profile.asp?id=" + id);

        SELECTORS.forEach();
        List<String> result = SELECTORS.stream()
                .map(string -> {
                    randomDelay(MIN_DELAY, MAX_DELAY);
                    String content = page.locator(string).innerText();
                    System.out.println(content);
                    return content;
                })
                .collect(Collectors.toList());;

        return result;
    }

    public Author getAuthor(int id) {
        List<String> authorContent = getAuthorContent(id);
        Pattern pattern = Pattern.compile("\\d+");
        if (!authorContent.isEmpty()) {
            try {
                 return new Author(
                         id,
                         authorContent.get(0),
                         Integer.parseInt(authorContent.get(1)),
                         Integer.parseInt(pattern.matcher(authorContent.get(2)).group(1)),
                         Integer.parseInt(authorContent.get(3))
                 );
            } catch (NumberFormatException e) {
                throw new RuntimeException("Ошибка при создании объекта Author: неверные поля Integer");
            }
        } else {
            throw new RuntimeException("Ошибка при создании объекта Author: неверные данные");
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
