package ru.core.accessories.state;

import org.bukkit.entity.Player;

/**
 * Источник маны для условий mana и mana_regen.
 * Используется ConditionRegistry и заменяется будущей интеграцией с плагином маны.
 * Для нового провайдера реализуйте два метода и передайте его в EffectEngine.
 */
public interface ManaProvider {
    double getMana(Player player);
    double getManaRegen(Player player);

    default void addMaxModifier(Player player, String key, double amount) {}
    default void removeMaxModifier(Player player, String key) {}
    default void addRegenModifier(Player player, String key, double amount) {}
    default void removeRegenModifier(Player player, String key) {}
}