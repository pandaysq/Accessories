package ru.core.accessories.config;

import java.util.Map;

/**
 * Сохраняет тип и параметры условия из конфигурации.
 * Используется ConditionRegistry для создания исполняемого Condition.
 * Новое условие добавляется в ConditionRegistry, а формат его параметров остаётся здесь.
 */
public record ConditionDefinition(String type, Map<String, Object> values) {
}