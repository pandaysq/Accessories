package ru.core.accessories.config;

import java.util.List;

/**
 * Описывает набор условий, атрибутов и зелий одного эффекта.
 * Используется EffectEngine при каждом цикле проверки условий.
 * Новые типы эффекта добавляются полями записи и применением в EffectEngine.
 */
public record EffectGroup(
        String key,
        String mode,
        int durationAfterTrigger,
        List<ConditionDefinition> conditions,
        List<AttributeDefinition> attributes,
        List<StatDefinition> stats,
        List<PotionDefinition> potions
) {
    public record AttributeDefinition(String attribute, String operation, double amount) {}
    public record StatDefinition(String stat, String operation, double amount) {}
    public record PotionDefinition(String type, int amplifier, boolean particles, boolean icon) {}
}