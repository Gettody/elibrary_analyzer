package elibraryparser;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Реализация интерфейса {@link ElibraryParser} для получения данных об авторах с сайта elibrary.ru через HTTP запросы.
 * Использует библиотеку RestAssured для выполнения HTTP запросов и регулярные выражения для парсинга HTML контента.
 */
@Log4j2
public class ElibraryParserHttp implements ElibraryParser {

    private static final String BASE_URL = "https://www.elibrary.ru/author_profile.asp?authorid=";
    private static String webProxyUrl;

    /**
     * Конструктор по умолчанию для {@code ElibraryParserHttp}.
     * Инициализирует парсер без использования веб-прокси.
     */
    public ElibraryParserHttp() {
        log.info("Инициализация ElibraryParserHttp без параметров");
    }

    /**
     * Конструктор для {@code ElibraryParserHttp} с возможностью указания URL веб-прокси.
     *
     * @param webProxyUrl URL веб-прокси сервера. Если {@code null} или пустой, прокси не используется.
     */
    public ElibraryParserHttp(String webProxyUrl) {
        log.info("Инициализация ElibraryParserHttp с webProxyUrl: {}", webProxyUrl);
        this.webProxyUrl = webProxyUrl;
    }

    /**
     * Получает информацию об авторе по его ID.
     *
     * @param authorId ID автора на elibrary.ru.
     * @return Объект {@link Author} с информацией об авторе или {@code null}, если не удалось получить данные.
     */
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

    /**
     * Извлекает основные данные об авторе со страницы профиля на elibrary.ru.
     *
     * @param authorId ID автора.
     * @return {@code Map<String, String>} с извлеченными данными об авторе или {@code null} в случае ошибки.
     */
    public Map<String, String> scrapeAuthorData(String authorId) {
        String url = (webProxyUrl != null ? webProxyUrl : "") + BASE_URL + authorId;
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

    /**
     * Загружает HTML контент страницы по указанному URL.
     *
     * @param url URL страницы для загрузки.
     * @return HTML контент страницы в виде строки или {@code null} в случае ошибки.
     */
    protected String downloadPage(String url) {
        try {
            Response response = RestAssured.get(url);

            if (response.getStatusCode() != 200) {
                log.warn("Сервер вернул код ответа: {} для URL: {}", response.getStatusCode(), url);
                return null;
            }

            String content = response.getBody().asString();
            log.debug("Страница успешно загружена");
            return content;

        } catch (Exception e) {
            log.error("Ошибка при загрузке страницы: " + e.getMessage());
            return null;
        }
    }

    /**
     * Извлекает данные об авторе из HTML контента страницы с использованием регулярных выражений.
     *
     * @param pageContent HTML контент страницы профиля автора.
     * @return {@code Map<String, String>} с извлеченными данными об авторе.
     */
    private Map<String, String> extractData(String pageContent) {
        Map<String, String> data = new HashMap<>();

        // Регулярные выражения и их описания
        Map<String, String> regexMap = new HashMap<>();
        regexMap.put("<title>(.+?) - Анализ публикационной активности</title>", "ФИО");
        regexMap.put(">Число публикаций на elibrary\\.ru<\\/font><\\/td><td align=\"center\" class=\"midtext\"><font color=\"#000000\"><a[^>]*>(\\d+)<\\/a>", "Число публикаций");
        regexMap.put(">Индекс Хирша по всем публикациям на elibrary\\.ru<\\/font><\\/td><td align=\"center\" class=\"midtext\"><font color=\"#000000\">(\\d+)<\\/font>", "Индекс Хирша");
        regexMap.put(">Число публикаций автора, процитированных хотя бы один раз<\\/font><\\/td><td align=\"center\" class=\"midtext\"><font color=\"#000000\">(\\d+)[^<]+<\\/font>", "Число цитирований");

        for (Map.Entry<String, String> entry : regexMap.entrySet()) {
            String regex = entry.getKey();
            String fieldName = entry.getValue();

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(pageContent);

            if (matcher.find()) {
                data.put(fieldName, matcher.group(1).trim());
                log.debug("Извлечено значение '{}': '{}'", fieldName, matcher.group(1).trim());
            } else {
                log.warn("Не удалось найти '{}' по regex: '{}'", fieldName, regex);
                data.put(fieldName, null);
            }
        }

        return data;
    }
}