package ru.core.accessories.stats;

import ru.core.accessories.config.EffectGroup;

import java.util.List;

/**
 * Вычисляет итог custom stat по формуле из ТЗ:
 * (сумма ADD_NUMBER) * (1 + сумма ADD_SCALAR) *
 * произведение (1 + MULTIPLY_SCALAR_1).
 * Используется AccessoryStatsService; новую арифметику параметров добавляйте здесь.
 */
public final class StatCalculator {
    public double calculate(List<EffectGroup.StatDefinition> definitions) {
        double addNumber = 0.0;
        double addScalar = 0.0;
        double multiply = 1.0;
        for (EffectGroup.StatDefinition definition : definitions) {
            switch (definition.operation().toUpperCase()) {
                case "ADD_SCALAR" -> addScalar += definition.amount();
                case "MULTIPLY_SCALAR_1" -> multiply *= 1.0 + definition.amount();
                default -> addNumber += definition.amount();
            }
        }
        return addNumber * (1.0 + addScalar) * multiply;
    }
}