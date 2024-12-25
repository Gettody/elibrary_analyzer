package elibraryparser;

import com.microsoft.playwright.*;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ElibraryParserOCR {
    private Playwright playwright;
    private final Browser browser;
    private static final Integer MIN_DELAY = 1000;
    private static final Integer MAX_DELAY = 3000;
    private static final int MAX_WAIT_TIME_SEC = 30;
    private static final String BASE_URL = "https://www.elibrary.ru/author_profile.asp?id=";
    private static final Path TESSDATA_PATH = Paths.get("tessdata");
    private static final List<String> KEYWORDS = Arrays.asList("Число публикаций на elibrary.ru","Индекс Хирша по всем публикациям на elibrary.ru", "Число публикаций автора, процитированных хотя бы один раз");


    public ElibraryParserOCR() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));

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
            waitForCorrectUrl(page, navigatedUrl);

            byte[] screenshotBytes = page.screenshot();
            BufferedImage screenshot = ImageIO.read(new ByteArrayInputStream(screenshotBytes));

            Map<String, String> results = new HashMap<>();
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(TESSDATA_PATH.toString());
            String ocrText = tesseract.doOCR(screenshot);

            for (String keyword : KEYWORDS) {
                String value = extractNumberFromRight(ocrText, keyword);
                results.put(keyword, value);
            }
            return results;
        } catch (PlaywrightException | IOException | TesseractException e) {
            System.out.println("Произошла ошибка при запросе или извлечении данных для автора " + id + ": " + e.getMessage());
            return null;
        }
    }
    private String extractNumberFromRight(String ocrText, String keyword){
        Pattern pattern = Pattern.compile(Pattern.quote(keyword) + "\\s*([\\d.,]+)");
        Matcher matcher = pattern.matcher(ocrText);

        if(matcher.find()){
            return matcher.group(1);
        }
        return "0";
    }



    private void waitForCorrectUrl(Page page, String expectedUrl) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(MAX_WAIT_TIME_SEC)) {
            String currentUrl = page.url();
            if (currentUrl.startsWith(expectedUrl)) {
                return;
            }
            randomDelay(1000, 2000);
        }
        throw new RuntimeException(String.format("Превышено время ожидания перенаправления на корректный URL %s", expectedUrl));
    }


    public Author getAuthor(int id) {
        Map<String, String> authorContent = getAuthorContent(id);
        try {
            String authorName = "Неизвестно"; // Имя через OCR будет сложнее получить, поэтому ставим значение по умолчанию
            int publishesCount = Integer.parseInt(authorContent.get("Число публикаций на elibrary.ru").replaceAll("[^0-9]", ""));
            int citedPublishesCount = Integer.parseInt(authorContent.get("Число публикаций автора, процитированных хотя бы один раз").replaceAll("[^0-9]", ""));
            int hirshIndex =  Integer.parseInt(authorContent.get("Индекс Хирша по всем публикациям на elibrary.ru").replaceAll("[^0-9]", ""));
            return new Author(id, authorName, publishesCount, publishesCount - citedPublishesCount, hirshIndex);
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
