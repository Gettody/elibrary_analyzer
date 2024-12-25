//package elibraryparser;
//
//import com.microsoft.playwright.*;
//import com.microsoft.playwright.Page;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class ElibraryParser {
//    private Playwright playwright;
//    private final Browser browser;
//    private static final Integer MIN_DELAY = 1000;
//    private static final Integer MAX_DELAY = 3000;
//    private static final String BASE_URL = "https://www.elibrary.ru/author_profile.asp?id=";
//    private static final Map<String, String> SELECTORS = new HashMap<String, String>();
//
//    public ElibraryParser() {
//        playwright = Playwright.create();
//        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)); // Запуск в headless режиме по умолчанию
//        SELECTORS.put(
//                "authorName",
//                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(3) > td:nth-child(1) > table:nth-child(1) > tbody > tr > td > div > font:nth-child(1) > b"
//        );
//        SELECTORS.put(
//                "publishesCount",
//                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(3) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(4) > td:nth-child(3) > font > a"
//        );
//        SELECTORS.put(
//                "noZeroCittPublishesCount",
//                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(3) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(18) > td:nth-child(3) > font"
//        );
//        SELECTORS.put(
//                "hirshIndex",
//                "#thepage > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td:nth-child(2) > form > table > tbody > tr:nth-child(3) > td:nth-child(1) > table:nth-child(5) > tbody > tr:nth-child(12) > td:nth-child(3) > font"
//        );
//    }
//
//    public ElibraryParser(Map<String, String> customSelectors) {
//        playwright = Playwright.create();
//        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)); // Запуск в headless режиме по умолчанию
//        SELECTORS.putAll(customSelectors);
//    }
//
//    private Page navigateToPage(int id) {
//
//    }
//
//    private Map<String, String> getAuthorContent(int id) {
//        randomDelay(MIN_DELAY, MAX_DELAY);
//        Page page = null;
//        try {
//            page = browser.newPage(new Browser.NewPageOptions().setUserAgent(getRandomUserAgent()));
//            final String navigatedUrl = BASE_URL + id;
//
//            page.onResponse(response -> {
//                if (response.status() == 500) {
//                    throw new PlaywrightException(String.format("Автор с authorId: %d в системе РИЦН не существует!", id));
//                }
//            });
//
//
//            page.navigate(navigatedUrl);
//
//            if(isCaptchaPage(page)) {
//                handleCaptcha(page); // Обработка капчи
//            }
//
//
//            Map<String, String> results = SELECTORS.entrySet().stream()
//                    .collect(Collectors.toMap(
//                            Map.Entry::getKey,
//                            entry -> {
//                                randomDelay(MIN_DELAY, MAX_DELAY);
//                                try {
//                                    return page.locator(entry.getValue()).innerText();
//                                } catch (PlaywrightException e) {
//                                    throw new PlaywrightException("Ошибка при извлечении данных: " + e.getMessage() + " url " + navigatedUrl);
//                                }
//                            }
//                    ));
//            return results;
//        } catch (PlaywrightException e) {
//            System.out.println("Произошла ошибка при запросе или извлечении данных для автора " + id + ": " + e.getMessage());
//            return null;
//        } finally {
//            if (page != null) {
//                page.close();
//            }
//        }
//    }
//    private boolean isCaptchaPage(Page page) {
//        try {
//            return page.locator("input[name='checkcode']").isVisible();
//        } catch (PlaywrightException e) {
//            return false;
//        }
//    }
//
//    private void handleCaptcha(Page page) {
//        System.out.println("Обнаружена капча. Открываем браузер для решения.");
//        browser.context().browser().newContext(new Browser.NewContextOptions().setHeadless(false));
//        page.context().browser().newPage(new Browser.NewPageOptions().setUserAgent(getRandomUserAgent()));
//        page.context().browser().pages().get(0).bringToFront();
//        System.out.println("Пожалуйста, решите капчу в браузере.");
//
//        // Ждем пока не появится нужная нам страница(произойдет редирект)
//        page.waitForURL("**/author_profile.asp?id=**");
//        browser.context().browser().newContext(new Browser.NewContextOptions().setHeadless(true));
//        System.out.println("Капча решена. Продолжаем работу в headless режиме.");
//    }
//
//
//    public Author getAuthor(int id) {
//        Map<String, String> authorContent = getAuthorContent(id);
//        if(authorContent == null) return null; // если запрос не выполнился или была ошибка, пропускаем автора
//
//        try {
//            String authorName = authorContent.get("authorName");
//            int publishesCount = Integer.parseInt(authorContent.get("publishesCount"));
//            int zeroCittPublishesCount = publishesCount - Integer.parseInt(authorContent.get("noZeroCittPublishesCount").split(" ")[0]);
//            int hirshIndex =  Integer.parseInt(authorContent.get("hirshIndex"));
//            return new Author(id, authorName, publishesCount, zeroCittPublishesCount, hirshIndex);
//        } catch (RuntimeException e) {
//            throw new RuntimeException("Ошибка при создании объекта Author: ошибка парсинга Integer");
//        }
//    }
//
//    private static void randomDelay(int min, int max) {
//        Random random = new Random();
//        int delay = random.nextInt(min, max);
//        try {
//            Thread.sleep(delay);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    private static String getRandomUserAgent() {
//        List<String> userAgents = Arrays.asList(
//                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
//                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
//                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
//                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
//                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/123.0.2420.53 Safari/537.36"
//        );
//        Random random = new Random();
//        return userAgents.get(random.nextInt(userAgents.size()));
//    }
//}
