package elibraryparser;

public record Author(int authorId, String name, int publishesCount, int zeroCittPublishesCount, int hirshIndex) {
    @Override
    public String toString() {
        return String.format("authorId: %d ФИО: %s Кол-во статей: %d Кол-во статей с нулевым цитированием: %d Индекс Хирша: %d",
                authorId, name, publishesCount, zeroCittPublishesCount, hirshIndex);
    }
}
