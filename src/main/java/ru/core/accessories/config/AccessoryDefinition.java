package ru.core.accessories.config;

import java.util.List;

/**
 * Неизменяемое описание одного аксессуара из accessories.yml.
 * Используется реестром, меню, командой выдачи и EffectEngine.
 * Новое свойство предмета добавляется полем записи и чтением в AccessoryConfig.
 */
public record AccessoryDefinition(
        String id,
        String name,
        List<String> lore,
        SlotType type,
        String material,
        int modelNumber,
        int deathKeepChance,
        double manaMaxBonus,
        double manaRegenBonus,
        List<EffectGroup> effects
) {
    public enum SlotType {
        NECKLACE, EMBLEM, RING
    }
}