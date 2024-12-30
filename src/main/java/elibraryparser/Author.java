package elibraryparser;

/**
 *  Представляет сущность "Автор" с информацией о его публикациях и индексе Хирша.
 *  Этот класс является record, что означает, что он автоматически генерирует конструктор,
 *  геттеры и методы equals(), hashCode() и toString().
 *
 *  @param authorId                Уникальный идентификатор автора.
 *  @param name                    Полное имя автора.
 *  @param publishesCount          Общее количество публикаций автора.
 *  @param zeroCittPublishesCount Количество публикаций автора, не имеющих цитирований.
 *  @param hirshIndex              Индекс Хирша автора.
 */
public record Author(int authorId, String name, int publishesCount, int zeroCittPublishesCount, int hirshIndex) {

    /**
     * Переопределенный метод {@link #toString()} для представления объекта {@code Author}
     * в виде строки с форматированной информацией.
     *
     * @return Строковое представление объекта {@code Author}, содержащее
     *          идентификатор автора, имя, количество публикаций, количество публикаций без цитирований
     *          и индекс Хирша.
     */
    @Override
    public String toString() {
        return String.format("authorId: %d ФИО: %s Кол-во статей: %d Кол-во статей с нулевым цитированием: %d Индекс Хирша: %d",
                authorId, name, publishesCount, zeroCittPublishesCount, hirshIndex);
    }
}