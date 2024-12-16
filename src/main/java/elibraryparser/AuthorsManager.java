package elibraryparser;

import org.jsoup.Jsoup;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AuthorsManager {
    private final ElibraryParser parser = new ElibraryParser();
    private final DatabaseManager database = new DatabaseManager();

    public List<Author> getAuthors(Set<Integer> authorIds) {
        List<Author> authors = authorIds.stream()
                .map(authorId -> {
                    if (!database.recordExists(authorId)) {
                        Author author = parser.getAuthor(authorId);
                        database.addAuthor(author);
                        return author;
                    }
                    return database.getAuthor(authorId);
                })
                .filter(Objects::nonNull).toList();
        return authors;
    }
}


