package elibraryparser;

import java.util.Map;

public record Author(int authorId, String name, int publishes, int zeroCittPublishes, int hIndex) {}
