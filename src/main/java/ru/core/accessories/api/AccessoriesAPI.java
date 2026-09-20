package ru.core.accessories.api;

import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Публичный API custom stats Accessories, зарегистрированный через ServicesManager.
 * Используется другими плагинами для чтения готового кэшированного результата;
 * новые методы публичной интеграции добавляются в этот интерфейс.
 */
public interface AccessoriesAPI {
    double getStat(Player player, String stat);
    Map<String, Double> getStats(Player player);
    boolean hasAccessory(Player player, String accessoryId);
}