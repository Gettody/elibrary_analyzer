package elibraryparser;

import org.jsoup.Jsoup;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthorsManager {
    private final ElibraryParser parser = new ElibraryParser();
    private final DatabaseManager database = new DatabaseManager();

    public Set<Author> getAuthors(Set<Integer> authorIds) {
        Set<Author> authors = authorIds.stream()
                .map(authorId -> {
                    if (!database.recordExists(authorId)) {
                        try {
                            Author author = parser.getAuthor(authorId);
                            database.addAuthor(author);
                            return author;
                        } catch (RuntimeException e) {
                            return null;
                        }
                    }
                    return database.getAuthor(authorId);
                })
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return authors;
    }
}


