package ru.core.accessories.condition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сравнивает число с форматом из конфига: 8, >=8, <=8, >8, <8 или 8..12.
 * Используется числовыми условиями.
 * Новые форматы сравнения добавляются в parse.
 */
public final class NumberMatcher {
    private static final Pattern COMPARISON = Pattern.compile("(>=|<=|>|<|=)?\\s*(-?\\d+(?:\\.\\d+)?)");

    private NumberMatcher() {}

    public static boolean matches(double actual, String expression) {
        if (expression == null) return false;
        String value = expression.trim();
        if (value.contains("..")) {
            String[] parts = value.split("\\.\\.", 2);
            return actual >= Double.parseDouble(parts[0]) && actual <= Double.parseDouble(parts[1]);
        }
        Matcher matcher = COMPARISON.matcher(value);
        if (!matcher.matches()) return false;
        double expected = Double.parseDouble(matcher.group(2));
        return switch (matcher.group(1) == null ? "=" : matcher.group(1)) {
            case ">=" -> actual >= expected;
            case "<=" -> actual <= expected;
            case ">" -> actual > expected;
            case "<" -> actual < expected;
            default -> Math.abs(actual - expected) < 0.0001;
        };
    }
}