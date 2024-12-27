package elibraryparser;

public interface ElibraryParser {
    Author getAuthor(int authorId);

    void close();
}
