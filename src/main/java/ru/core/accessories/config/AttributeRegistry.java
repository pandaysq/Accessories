package ru.core.accessories.config;

import org.bukkit.attribute.Attribute;

import java.util.Locale;

/**
 * Преобразует имена атрибутов и операций из YAML в API Paper.
 * Используется EffectEngine при добавлении AttributeModifier.
 * Новый алиас атрибута добавляется в switch этого класса.
 */
public final class AttributeRegistry {
    private AttributeRegistry() {}

    public static Attribute attribute(String name) {
        String key = name.toUpperCase(Locale.ROOT);
        return switch (key) {
            case "MAX_HEALTH" -> Attribute.MAX_HEALTH;
            case "ATTACK_DAMAGE" -> Attribute.ATTACK_DAMAGE;
            case "ATTACK_SPEED" -> Attribute.ATTACK_SPEED;
            case "ARMOR" -> Attribute.ARMOR;
            case "ARMOR_TOUGHNESS" -> Attribute.ARMOR_TOUGHNESS;
            case "KNOCKBACK_RESISTANCE" -> Attribute.KNOCKBACK_RESISTANCE;
            case "MOVEMENT_SPEED" -> Attribute.MOVEMENT_SPEED;
            case "LUCK" -> Attribute.LUCK;
            case "ENTITY_INTERACTION_RANGE" -> Attribute.ENTITY_INTERACTION_RANGE;
            case "BLOCK_INTERACTION_RANGE" -> Attribute.BLOCK_INTERACTION_RANGE;
            case "GRAVITY" -> Attribute.GRAVITY;
            case "SAFE_FALL_DISTANCE" -> Attribute.SAFE_FALL_DISTANCE;
            case "STEP_HEIGHT" -> Attribute.STEP_HEIGHT;
            case "BURNING_TIME" -> Attribute.BURNING_TIME;
            default -> null;
        };
    }

    public static org.bukkit.attribute.AttributeModifier.Operation operation(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "ADD_SCALAR" -> org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR;
            case "MULTIPLY_SCALAR_1" -> org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;
        };
    }
}